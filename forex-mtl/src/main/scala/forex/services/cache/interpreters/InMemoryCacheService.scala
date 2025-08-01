package forex.services.cache.interpreters
import cats.Applicative
import forex.services.cache.Algebra

import java.time.Instant
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration

class InMemoryCacheService[F[_]: Applicative] extends Algebra[F] {
  private case class CacheEntry(value: String, expiresAt: Instant)
  private val store = TrieMap.empty[String, CacheEntry]

  override def set(key: String, value: String, ttl: FiniteDuration): Unit = {
    val expiresAt = Instant.now().plusMillis(ttl.toMillis)
    store.put(key, CacheEntry(value, expiresAt))
    ()
  }

  override def get(key: String): F[Option[String]] = {
    val now = Instant.now()
    store.get(key) match {
      case Some(CacheEntry(value, expiresAt)) if expiresAt.isAfter(now) => Applicative[F].pure(Some(value))
      case Some(_)                                                      =>
        store.remove(key)
        Applicative[F].pure(None)
      case None => Applicative[F].pure(None)
    }
  }
}
