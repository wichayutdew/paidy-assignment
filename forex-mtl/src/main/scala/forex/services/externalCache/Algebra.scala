package forex.services.externalCache

import forex.services.externalCache.errors._

import scala.concurrent.duration.FiniteDuration

trait Algebra[F[_]] {
  def set(key: String, value: String, ttl: FiniteDuration): F[Error Either String]
  def get(key: String): F[Error Either String]
}
