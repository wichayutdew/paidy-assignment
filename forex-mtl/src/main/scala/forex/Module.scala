package forex

import cats.effect.{ Concurrent, Timer }
import com.bettercloud.vault.{ Vault, VaultConfig }
import forex.config.models.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.programs.rates.{ OneFrameAPIProgram, OneFrameTokenProgram }
import forex.services._
import forex.services.cache.interpreters.{ InMemoryCacheService, RedisService }
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.{ RedisClient, RedisURI }
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

import java.time.Duration

class Module[F[_]: Concurrent: Timer](
    config: ApplicationConfig,
    httpClient: Client[F]
) {
  /* ------------------------------ SERVICES ------------------------------ */
  // Secret Manager
  private val vaultClient: Vault = {
    val vaultConfig: VaultConfig = new VaultConfig()
      .address(s"http://${config.client.vault.host}:${config.client.vault.port}")
      .token(config.client.vault.token)
      .openTimeout(config.client.vault.connectionTimeout.toSeconds.toInt)
      .readTimeout(config.client.vault.requestTimeout.toSeconds.toInt)
      .build()
    new Vault(vaultConfig)
  }
  private val secretManagerService: SecretManagerService[F] = SecretManagerServices.vault[F](vaultClient)

  // External Cache
  private val redisClient: RedisCommands[String, String] = {
    val password: CharSequence = config.client.redis.token
    val uri: RedisURI          = RedisURI.Builder
      .redis(config.client.redis.host, config.client.redis.port)
      .withPassword(password)
      .withTimeout(Duration.ofMillis(config.client.redis.connectionTimeout.toMillis))
      .build()
    val redisClient: RedisClient = RedisClient.create(uri)
    redisClient.connect().sync()
  }
  private val redisService: RedisService[F] = CacheServices.redis[F](redisClient)

  // In-Memory Cache
  private val inMemoryCacheService: InMemoryCacheService[F] = CacheServices.inMemory[F]

  // Rates
  private val ratesService: RatesService[F] = RatesServices.oneFrame[F](
    client = httpClient,
    config = config.client.oneFrame
  )

  /* ------------------------------ PROGRAMS ------------------------------ */
  val oneFrameTokenProgram: OneFrameTokenProgram[F] =
    new OneFrameTokenProgram[F](secretManagerService, inMemoryCacheService, config.cache.token)
  val oneFrameApiProgram: OneFrameAPIProgram[F] =
    new OneFrameAPIProgram[F](ratesService, redisService, config.cache.rates)
  val ratesProgram: RatesProgram[F] =
    RatesProgram[F](oneFrameTokenProgram, oneFrameApiProgram)

  /* ------------------------------ SERVER ------------------------------ */
  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes
  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]
  private val routesMiddleware: PartialMiddleware = (http: HttpRoutes[F]) => AutoSlash(http)
  private val appMiddleware: TotalMiddleware      = { http: HttpApp[F] => Timeout(config.server.requestTimeout)(http) }
  private val http: HttpRoutes[F]                 = ratesHttpRoutes
  val httpApp: HttpApp[F]                         = appMiddleware(routesMiddleware(http).orNotFound)
}
