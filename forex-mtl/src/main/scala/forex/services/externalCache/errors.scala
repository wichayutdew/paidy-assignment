package forex.services.externalCache

import forex.domain.core.BaseError

object errors {

  sealed trait Error extends BaseError
  object Error {
    final case class CachePutFailed(msg: String) extends Error
    final case class CacheExpired(msg: String) extends Error
    final case class CacheGetFailed(msg: String) extends Error
  }

}
