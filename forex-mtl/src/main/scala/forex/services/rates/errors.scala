package forex.services.rates
import forex.domain.core.BaseError
import forex.services.vault.errors.{ Error => VaultError }

object errors {

  sealed trait Error extends BaseError
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class ExchangeRateNotFound(msg: String) extends Error
    final case class DecodingFailure(msg: String) extends Error
    final case class InvalidToken(msg: String) extends Error
  }

  def toRatesError(error: VaultError): Error = error match {
    case VaultError.SecretLookupFailed(msg) => Error.InvalidToken(msg)
  }

}
