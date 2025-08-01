package forex.programs.rates
import forex.domain.core.BaseError
import forex.services.rates.errors.{ Error => RatesServiceError }
import forex.services.secretManager.errors.{ Error => SecretManagerServiceError }

object errors {

  sealed trait Error extends BaseError
  object Error {
    final case class RateLookupFailed(msg: String) extends Error
    final case class ExchangeRateNotFound(msg: String) extends Error
    final case class DecodingFailure(msg: String) extends Error
  }

  def toProgramError(error: BaseError): Error = error match {
    case RatesServiceError.OneFrameLookupFailed(msg)       => Error.RateLookupFailed(msg)
    case SecretManagerServiceError.SecretLookupFailed(msg) => Error.RateLookupFailed(msg)
    case RatesServiceError.ExchangeRateNotFound(msg)       => Error.ExchangeRateNotFound(msg)
    case RatesServiceError.DecodingFailure(msg)            => Error.DecodingFailure(msg)
  }
}
