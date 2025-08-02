package forex

import cats.data.EitherT
import cats.effect.{ Concurrent, ConcurrentEffect, Resource, Timer }
import cats.implicits.toFunctorOps
import cats.syntax.semigroupk._
import com.bettercloud.vault.{ Vault, VaultConfig }
import forex.config.models.ApplicationConfig
import forex.domain.vault.Constant
import forex.http.cache.CacheHttpRoutes
import forex.http.healthCheck.HealthCheckHttpRoutes
import forex.http.rates.RatesHttpRoutes
import forex.programs._
import forex.programs.rates.{ OneFrameAPIProgram, OneFrameTokenProgram }
import forex.services._
import forex.services.cache.interpreters.{ InMemoryCacheService, RedisService }
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.{ RedisClient, RedisURI }
import org.http4s._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

import java.time.Duration
import scala.concurrent.ExecutionContext

class Module[F[_]: ConcurrentEffect: Timer](
    config: ApplicationConfig,
    httpClient: Client[F],
    secretManagerService: SecretManagerService[F],
    redisService: RedisService[F]
) {
  /* ------------------------------ SERVICES ------------------------------ */
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

  val cacheProgram: CacheProgram[F] = CacheProgram[F](redisService, inMemoryCacheService)

  /* ------------------------------ SERVER ------------------------------ */
  private val ratesHttpRoutes: HttpRoutes[F]       = new RatesHttpRoutes[F](ratesProgram).routes
  private val healthCheckHttpRoutes: HttpRoutes[F] = new HealthCheckHttpRoutes[F]().routes
  private val cacheHttpRoutes: HttpRoutes[F]       = new CacheHttpRoutes[F](cacheProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]
  private val routesMiddleware: PartialMiddleware = (http: HttpRoutes[F]) => AutoSlash(http)
  private val appMiddleware: TotalMiddleware      = { http: HttpApp[F] => Timeout(config.server.requestTimeout)(http) }

  private val allRoutes: HttpRoutes[F] = ratesHttpRoutes <+> healthCheckHttpRoutes <+> cacheHttpRoutes
  val httpApp: HttpApp[F]              = appMiddleware(routesMiddleware(allRoutes).orNotFound)
}

object Module {
  def apply[F[_]: ConcurrentEffect: Timer](config: ApplicationConfig, ec: ExecutionContext): Resource[F, Module[F]] = {
    def createRedisClient(password: String): Resource[F, RedisCommands[String, String]] =
      Resource
        .make(
          acquire = Concurrent[F].delay {
            val uri: RedisURI = RedisURI.Builder
              .redis(config.client.redis.host, config.client.redis.port)
              .withPassword(password.toCharArray)
              .withTimeout(Duration.ofMillis(config.client.redis.connectionTimeout.toMillis))
              .build()

            val client: RedisClient                                 = RedisClient.create(uri)
            val connection: StatefulRedisConnection[String, String] = client.connect()
            (client, connection)
          }
        )(
          release = { case (redisClient, connection) =>
            Concurrent[F].delay {
              connection.close()
              redisClient.shutdown()
            }.void
          }
        )
        .map(_._2.sync())

    for {
      httpClient <- BlazeClientBuilder[F](ec)
                      .withConnectTimeout(config.client.oneFrame.connectionTimeout)
                      .withRequestTimeout(config.client.oneFrame.requestTimeout)
                      .resource
      vaultConfig = new VaultConfig()
                      .address(s"http://${config.client.vault.host}:${config.client.vault.port}")
                      .token(config.client.vault.token)
                      .openTimeout(config.client.vault.connectionTimeout.toSeconds.toInt)
                      .readTimeout(config.client.vault.requestTimeout.toSeconds.toInt)
                      .build()
      secretManager = SecretManagerServices.vault[F](new Vault(vaultConfig))
      password <- Resource.eval(
                    EitherT(secretManager.get(Constant.Path.REDIS, Constant.Key.TOKEN))
                      .leftMap(error => new RuntimeException(s"Failed to fetch Redis password from Vault: $error"))
                      .rethrowT
                  )
      redisClient <- createRedisClient(password)
      redisService = CacheServices.redis[F](redisClient)
      module <- Resource.eval(Concurrent[F].delay(new Module[F](config, httpClient, secretManager, redisService)))
      _ <- Resource.eval(module.ratesProgram.preFetch()) // Pre-fetching data to warm up the cache
    } yield module
  }
}
