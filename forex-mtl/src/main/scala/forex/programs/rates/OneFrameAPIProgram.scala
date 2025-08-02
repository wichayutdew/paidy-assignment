package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import cats.implicits.{ toFlatMapOps, toFunctorOps }
import forex.config.models.CacheConfig
import forex.domain.rates.{ Currency, Pair, Rate }
import forex.programs.rates.errors.Error.DecodingFailure
import forex.programs.rates.errors.{ toProgramError, Error }
import forex.services.RatesService
import forex.services.cache.interpreters.RedisService
import io.circe.parser.decode
import io.circe.syntax.EncoderOps

class OneFrameAPIProgram[F[_]: Monad](
    ratesService: RatesService[F],
    redisService: RedisService[F],
    ratesCacheConfig: CacheConfig
) {
  def preFetch(token: String): F[Unit] =
    if (ratesCacheConfig.enabled) fetchRates(Currency.getAllPairs, token).void else Monad[F].pure(())

  def get(pair: Pair, token: String): F[Error Either Rate] =
    if (ratesCacheConfig.enabled) {
      val cacheKey = s"${ratesCacheConfig.prefix}_${pair.toCacheKey}".replaceAll("/", "_")
      redisService.get(cacheKey).flatMap {
        case Some(rateString) => fromJsonString(rateString)
        case None             => findRate(pair, fetchRates(List(pair), token))
      }
    } else findRate(pair, fetchRates(List(pair), token))

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

  private def fetchRates(pair: List[Pair], token: String): F[Error Either List[Rate]] = (for {
    rates <- EitherT(ratesService.get(pair, token)).leftMap(toProgramError)
  } yield rates.map { rate =>
    if (ratesCacheConfig.enabled) {
      val cacheKey = s"${ratesCacheConfig.prefix}_${rate.pair.toCacheKey}".replaceAll("/", "_")
      redisService.set(cacheKey, rate.asJson.noSpaces, ratesCacheConfig.ttl)
    }
    rate
  }).value
}
