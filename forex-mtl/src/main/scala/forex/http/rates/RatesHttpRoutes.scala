package forex.http
package rates

import cats.data.Validated.Valid
import cats.data.ValidatedNel
import cats.effect.Sync
import cats.syntax.flatMap._
import forex.domain.Currency
import forex.domain.core.BaseError
import forex.programs.RatesProgram
import forex.programs.rates.errors.{ Error => ProgramError }
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ HttpRoutes, ParseFailure, Response }

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(fromValidated) +& ToQueryParam(toValidated) =>
      (fromValidated, toValidated) match {
        case (Valid(from), Valid(to)) =>
          rates.get(RatesProgramProtocol.GetRatesRequest(from, to)).flatMap {
            case Right(rate) => Ok(rate.asGetApiResponse)
            case Left(error) => toHttpError(error)
          }
        case (fromValidated, toValidated) =>
          BadRequest(
            List(buildErrorMessage("from", fromValidated), buildErrorMessage("to", toValidated)).flatten
              .mkString(";\n")
          )
      }
  }

  private def buildErrorMessage(queryParam: String, validated: ValidatedNel[ParseFailure, Currency]): Option[String] =
    validated.swap
      .map(errors =>
        s"Invalid `$queryParam` parameter: ${errors.toList.map(error => s"${error.sanitized}: ${error.details}").mkString(", ")}"
      )
      .toOption

  private def toHttpError(error: BaseError): F[Response[F]] =
    error match {
      case ProgramError.ExchangeRateNotFound(pair) => NotFound(s"Exchange rate $pair is not found")
      case ProgramError.RateLookupFailed(error)    => BadGateway(error)
      case ProgramError.DecodingFailure(error)     => UnprocessableEntity(error)
    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

}
