package forex.programs.rates
import forex.domain.core.BaseError
import forex.services.rates.errors.{ Error => RatesServiceError }

object errors {

  sealed trait Error extends BaseError
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
  }

  def toProgramError(error: RatesServiceError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg) => Error.RateLookupFailed(msg)
  }
}
