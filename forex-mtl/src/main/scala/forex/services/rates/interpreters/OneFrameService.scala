package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps }
import forex.config.models.OneFrameConfig
import forex.domain.oneframe.RateDTO
import forex.domain.rates.{ Pair, Rate }
import forex.services.rates.Algebra
import forex.services.rates.errors.Error.{ DecodingFailure, ExchangeRateNotFound, OneFrameLookupFailed }
import forex.services.rates.errors._
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.typelevel.ci.CIStringSyntax

class OneFrameService[F[_]: Sync](
    client: Client[F],
    config: OneFrameConfig
) extends Algebra[F] {

  private val baseUri = Uri(
    Some(Uri.Scheme.http),
    Some(Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)))
  )

  override def get(pair: Pair): F[Error Either Rate] = getRates(List(pair)).flatMap {
    case Right(rateDTOs) if rateDTOs.nonEmpty => Sync[F].pure(Right(Rate.fromOneFrameDTO(rateDTOs.head)))
    case Right(_)                             => Sync[F].pure(Left(ExchangeRateNotFound(s"${pair.from} to ${pair.to}")))
    case Left(error)                          => Sync[F].pure(Left(error))
  }

  private def getRates(pairs: List[Pair]): F[Error Either List[RateDTO]] = {
    val parameterIdentifier: String      = "pair"
    val queryParams: Map[String, String] = pairs.map(pair => parameterIdentifier -> s"${pair.from}${pair.to}").toMap
    val path: String                     = "rates"
    val request                          = Request[F](
      method = GET,
      uri = baseUri / path withQueryParams queryParams,
      headers = Headers(Header.Raw(ci"token", config.token))
    )

    client.run(request).use { response =>
      implicit val rateListEntityDecoder: EntityDecoder[F, List[RateDTO]] = jsonOf[F, List[RateDTO]]

      (response.status match {
        case Ok =>
          response
            .as[List[RateDTO]]
            .map(rates => Right(rates): Error Either List[RateDTO])
        case _ =>
          response
            .as[String]
            .map(errorMsg => Left(OneFrameLookupFailed(errorMsg)): Error Either List[RateDTO])
      }).handleErrorWith { error =>
        val errorMsg = error match {
          case e: DecodeFailure => s"Failed to decode response: ${e.getMessage}"
          case e                => s"Unexpected error: ${e.getMessage}"
        }
        Sync[F].pure(Left(DecodingFailure(errorMsg)))
      }
    }
  }
}
