package forex.services.secretManager

import forex.domain.core.BaseError

object errors {

  sealed trait Error extends BaseError
  object Error {
    final case class SecretLookupFailed(msg: String) extends Error
  }

}
