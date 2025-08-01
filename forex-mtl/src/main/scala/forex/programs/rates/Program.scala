package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import cats.implicits.toFlatMapOps
import forex.config.models.CacheConfig
import forex.domain.rates.{ Pair, Rate }
import forex.domain.vault.Constant.{ Key, Path }
import forex.programs.rates.errors.Error.DecodingFailure
import forex.programs.rates.errors._
import forex.services.{ ExternalCacheService, RatesService, SecretManagerService }
import io.circe.syntax.EncoderOps
import io.circe.parser.decode

class Program[F[_]: Monad](
    ratesService: RatesService[F],
    secretManagerService: SecretManagerService[F],
    externalCacheService: ExternalCacheService[F],
    ratesCacheConfig: CacheConfig
) extends Algebra[F] {
  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val exchangeRatePair = Pair(request.from, request.to)
    if (ratesCacheConfig.enabled) {
      val cacheKey = s"${ratesCacheConfig.prefix}_${exchangeRatePair.toCacheKey}"
      externalCacheService.get(cacheKey).flatMap {
        case Some(rateString) => fromJsonString(rateString)
        case None             => fetchRate(exchangeRatePair, cacheKey, shouldFillCache = true)
      }
    } else fetchRate(exchangeRatePair, "", shouldFillCache = false)
  }

  private def fetchRate(pair: Pair, cacheKey: String, shouldFillCache: Boolean): F[Error Either Rate] = (for {
    token <- EitherT(secretManagerService.get(Path.ONE_FRAME, Key.TOKEN)).leftMap(toProgramError)
    rate <- EitherT(ratesService.get(pair, token)).leftMap(toProgramError)
  } yield {
    if (shouldFillCache) externalCacheService.set(cacheKey, rate.asJson.noSpaces, ratesCacheConfig.ttl)
    rate
  }).value

  private def fromJsonString(rateString: String): F[Error Either Rate] =
    decode[Rate](rateString) match {
      case Right(rate) => Monad[F].pure(Right(rate))
      case Left(error) => Monad[F].pure(Left(DecodingFailure(error.getMessage)))
    }
}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
      secretManagerService: SecretManagerService[F],
      externalCacheService: ExternalCacheService[F],
      ratesCacheConfig: CacheConfig
  ): Algebra[F] = new Program[F](ratesService, secretManagerService, externalCacheService, ratesCacheConfig)

}
