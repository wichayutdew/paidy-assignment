package forex.services.cache

import scala.concurrent.duration.FiniteDuration

trait Algebra[F[_]] {
  def set(key: String, value: String, ttl: FiniteDuration): Unit
  def get(key: String): F[Option[String]]
  def delete(key: String): Unit
}
