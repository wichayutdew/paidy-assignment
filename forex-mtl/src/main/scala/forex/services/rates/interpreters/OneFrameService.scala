package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps }
import forex.config.models.OneFrameConfig
import forex.domain.core.measurement.logging.{ AppLogger, ErrorLog }
import forex.domain.core.measurement.metrics.{ EventCounter, MeasurementHelper, MetricsTag, TimerMetric }
import forex.domain.oneframe.Constant.{ HEADER, MESSAGE, PATH, QUERY_PARAMETER }
import forex.domain.oneframe.{ OneFrameError, RateDTO }
import forex.domain.rates.{ Pair, Rate }
import forex.services.rates.Algebra
import forex.services.rates.errors.Error.{ DecodingFailure, OneFrameLookupFailed }
import forex.services.rates.errors._
import io.opentelemetry.api.metrics.Meter
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.typelevel.ci.CIString

class OneFrameService[F[_]: Sync](client: Client[F], config: OneFrameConfig)(implicit meter: Meter)
    extends Algebra[F]
    with AppLogger
    with MeasurementHelper {
  implicit val rateListEntityDecoder: EntityDecoder[F, List[RateDTO]]      = jsonOf[F, List[RateDTO]]
  implicit val oneFrameErrorEntityDecoder: EntityDecoder[F, OneFrameError] = jsonOf[F, OneFrameError]

  private val successRateCounter: EventCounter = EventCounter("client.success", "OneFrameService")
  private val timer                            = TimerMetric("client.time", "OneFrameService")

  private val baseUri = Uri(
    Some(Uri.Scheme.http),
    Some(Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)))
  )

  override def get(pairs: List[Pair], token: String): F[Either[Error, List[Rate]]] =
    measure(timer, Map(MetricsTag.OPERATION -> PATH.RATES)) {
      getRates(pairs, token).flatMap {
        case Right(rateDTOs) => Sync[F].pure(Right(rateDTOs.map(Rate.fromOneFrameDTO)))
        case Left(error)     => Sync[F].pure(Left(error))
      }
    }

  private def getRates(pairs: List[Pair], token: String): F[Error Either List[RateDTO]] = {
    def errorHandler(errorMessage: String, error: Option[Throwable]): F[Error Either List[RateDTO]] = {
      successRateCounter.record(Map(MetricsTag.STATUS -> false.toString, MetricsTag.OPERATION -> PATH.RATES))
      errorMessage match {
        case MESSAGE.RATE_LIMIT =>
          logger.log(ErrorLog(s"[One Frame API] Rate limit reached: $errorMessage", error))
          Sync[F].pure(Left(OneFrameLookupFailed("Rate limited")))
        case MESSAGE.FAILED_DECODE =>
          logger.log(ErrorLog(s"[One Frame API] Failed to decode response", error))
          Sync[F].pure(Left(DecodingFailure("Failed to decode response")))
        case _ =>
          logger.log(ErrorLog(s"[One Frame API] Unexpected error: $errorMessage", error))
          Sync[F].pure(Left(OneFrameLookupFailed("Unexpected Response from One Frame API")): Error Either List[RateDTO])
      }
    }

    val query: Query = Query.fromPairs(pairs.map(pair => QUERY_PARAMETER.PAIR -> s"${pair.from}${pair.to}"): _*)
    val request      = Request[F](
      method = GET,
      uri = (baseUri / PATH.RATES).copy(query = query),
      headers = Headers(Header.Raw(CIString(HEADER.TOKEN), token))
    )
    client.run(request).use { response =>
      response.status match {
        case Ok =>
          response.as[List[RateDTO]].attempt.flatMap {
            case Right(rates) =>
              successRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> PATH.RATES))
              Sync[F].pure(Right(rates))
            case Left(_) =>
              response.as[OneFrameError].attempt.flatMap {
                case Right(errorMsg) => errorHandler(errorMsg.error, None)
                case Left(error)     => errorHandler(MESSAGE.FAILED_DECODE, Some(error))
              }
          }
        case _ =>
          response.as[String].attempt.flatMap {
            case Right(errorMsg) => errorHandler(errorMsg, None)
            case Left(error)     => errorHandler(MESSAGE.FAILED_DECODE, Some(error))
          }
      }
    }
  }
}
