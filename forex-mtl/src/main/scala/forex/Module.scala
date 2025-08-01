package forex

import cats.effect.{ Concurrent, Timer }
import com.bettercloud.vault.{ Vault, VaultConfig }
import forex.config.models.ApplicationConfig
import forex.domain.vault.Constant.{ Key, Path }
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.services._
import forex.services.vault.interpreters.VaultService
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Concurrent: Timer](
    config: ApplicationConfig,
    httpClient: Client[F]
) {

  private val vaultConfig: VaultConfig = new VaultConfig()
    .address(s"http://${config.client.vault.host}:${config.client.vault.port}")
    .token(config.client.vault.token)
    .openTimeout(config.client.vault.connectionTimeout.toSeconds.toInt)
    .readTimeout(config.client.vault.requestTimeout.toSeconds.toInt)
    .build()
  private val vaultClient: Vault            = new Vault(vaultConfig)
  private val vaultService: VaultService[F] = VaultServices.vault[F](vaultClient)

  private val ratesService: RatesService[F] = RatesServices.oneFrame[F](
    client = httpClient,
    config = config.client.oneFrame,
    token = vaultService.get(Path.ONE_FRAME, Key.TOKEN)
  )

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = (http: HttpRoutes[F]) => AutoSlash(http)

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] => Timeout(config.server.requestTimeout)(http) }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
