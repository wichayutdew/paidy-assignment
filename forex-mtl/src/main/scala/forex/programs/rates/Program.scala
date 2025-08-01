package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import cats.implicits.{ toFlatMapOps, toFunctorOps }
import forex.config.models.CacheConfig
import forex.domain.rates.{ Currency, Pair, Rate }
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
  override def preFetch(): F[Unit] = fetchRates(Currency.getAllPairs, shouldFillCache = true).void

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val exchangeRatePair = Pair(request.from, request.to)
    if (ratesCacheConfig.enabled) {
      val cacheKey = s"${ratesCacheConfig.prefix}_${exchangeRatePair.toCacheKey}"
      externalCacheService.get(cacheKey).flatMap {
        case Some(rateString) => fromJsonString(rateString)
        case None             => findRate(exchangeRatePair, fetchRates(List(exchangeRatePair), shouldFillCache = true))
      }
    } else
      findRate(exchangeRatePair, fetchRates(List(exchangeRatePair), shouldFillCache = false))
  }

  private def fromJsonString(rateString: String): F[Error Either Rate] =
    decode[Rate](rateString) match {
      case Right(rate) => Monad[F].pure(Right(rate))
      case Left(error) => Monad[F].pure(Left(DecodingFailure(error.getMessage)))
    }

  private def findRate(pair: Pair, ratesF: F[Error Either List[Rate]]): F[Error Either Rate] =
    ratesF.flatMap {
      case Right(rates) =>
        Monad[F].pure(
          rates
            .find(_.pair == pair)
            .map(Right(_))
            .getOrElse(Left(Error.ExchangeRateNotFound(s"${pair.from} to ${pair.to}")))
        )
      case Left(error) => Monad[F].pure(Left(error))
    }

  private def fetchRates(pair: List[Pair], shouldFillCache: Boolean): F[Error Either List[Rate]] = (for {
    token <- EitherT(secretManagerService.get(Path.ONE_FRAME, Key.TOKEN)).leftMap(toProgramError)
    rates <- EitherT(ratesService.get(pair, token)).leftMap(toProgramError)
  } yield rates.map { rate =>
    if (shouldFillCache) {
      val cacheKey = s"${ratesCacheConfig.prefix}_${rate.pair.toCacheKey}"
      externalCacheService.set(cacheKey, rate.asJson.noSpaces, ratesCacheConfig.ttl)
    }
    rate
  }).value
}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
      secretManagerService: SecretManagerService[F],
      externalCacheService: ExternalCacheService[F],
      ratesCacheConfig: CacheConfig
  ): Program[F] = new Program[F](ratesService, secretManagerService, externalCacheService, ratesCacheConfig)

}
