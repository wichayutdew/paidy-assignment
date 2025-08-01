package forex.services.rates.interpreters

import cats.effect.Sync
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps }
import forex.config.models.OneFrameConfig
import forex.domain.oneframe.Constant.{ HEADER, PATH, QUERY_PARAMETER }
import forex.domain.oneframe.RateDTO
import forex.domain.rates.{ Pair, Rate }
import forex.services.rates.Algebra
import forex.services.rates.errors.Error.{ DecodingFailure, OneFrameLookupFailed }
import forex.services.rates.errors._
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.typelevel.ci.CIString

class OneFrameService[F[_]: Sync](client: Client[F], config: OneFrameConfig) extends Algebra[F] {
  implicit val rateListEntityDecoder: EntityDecoder[F, List[RateDTO]] = jsonOf[F, List[RateDTO]]

  private val baseUri = Uri(
    Some(Uri.Scheme.http),
    Some(Uri.Authority(host = Uri.RegName(config.host), port = Some(config.port)))
  )

  override def get(pairs: List[Pair], token: String): F[Either[Error, List[Rate]]] = getRates(pairs, token).flatMap {
    case Right(rateDTOs) => Sync[F].pure(Right(rateDTOs.map(Rate.fromOneFrameDTO)))
    case Left(error)     => Sync[F].pure(Left(error))
  }

  private def getRates(pairs: List[Pair], token: String): F[Error Either List[RateDTO]] = {
    val queryParams: Map[String, String] = pairs.map(pair => QUERY_PARAMETER.PAIR -> s"${pair.from}${pair.to}").toMap
    val request                          = Request[F](
      method = GET,
      uri = baseUri / PATH.RATES withQueryParams queryParams,
      headers = Headers(Header.Raw(CIString(HEADER.TOKEN), token))
    )

    client.run(request).use { response =>
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
