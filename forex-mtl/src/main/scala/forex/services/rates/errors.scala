package forex.services.rates
import forex.domain.core.BaseError

object errors {

  sealed trait Error extends BaseError
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class ExchangeRateNotFound(msg: String) extends Error
    final case class DecodingFailure(msg: String) extends Error
  }
}
