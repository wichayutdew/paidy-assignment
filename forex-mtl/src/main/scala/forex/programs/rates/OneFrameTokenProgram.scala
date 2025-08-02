package forex.programs.rates
import cats.Monad
import cats.data.EitherT
import cats.implicits.toFlatMapOps
import forex.config.models.CacheConfig
import forex.domain.vault.Constant.{ Key, Path }
import forex.programs.rates.errors.{ toProgramError, Error }
import forex.services.SecretManagerService
import forex.services.cache.interpreters.InMemoryCacheService

class OneFrameTokenProgram[F[_]: Monad](
    secretManagerService: SecretManagerService[F],
    inMemoryCacheService: InMemoryCacheService[F],
    tokenCacheConfig: CacheConfig
) {
  private val cacheKey = s"${tokenCacheConfig.prefix}_${Path.ONE_FRAME}".replaceAll("/", "_")

  def getToken: F[Error Either String] = if (tokenCacheConfig.enabled) {
    inMemoryCacheService.get(cacheKey).flatMap {
      case Some(token) => Monad[F].pure(Right(token))
      case None        => fetchAndFillCache
    }
  } else fetchAndFillCache

  private def fetchAndFillCache: F[Either[Error, String]] = (for {
    token <- EitherT(secretManagerService.get(Path.ONE_FRAME, Key.TOKEN)).leftMap(toProgramError)
  } yield {
    if (tokenCacheConfig.enabled) { inMemoryCacheService.set(cacheKey, token, tokenCacheConfig.ttl) }
    token
  }).value
}
