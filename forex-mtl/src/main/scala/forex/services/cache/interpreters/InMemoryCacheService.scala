package forex.services.cache.interpreters
import cats.Applicative
import forex.services.cache.Algebra

import java.time.Instant
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class InMemoryCacheService[F[_]: Applicative] extends Algebra[F] {
  private case class CacheEntry(value: String, expiresAt: Instant)
  private val store = TrieMap.empty[String, CacheEntry]

  override def set(key: String, value: String, ttl: FiniteDuration): Unit = {
    val expiresAt = Instant.now().plusMillis(ttl.toMillis)
    Try(store.put(key, CacheEntry(value, expiresAt))).toEither match {
      case Right(Some(_)) =>
      case Right(_)       => // TODO: add log/metric to measure cache put fail rate
      case Left(_)        => // TODO: add log/metric to measure cache put fail with error
    }
  }

  override def get(key: String): F[Option[String]] = {
    val now = Instant.now()
    store.get(key) match {
      case Some(CacheEntry(value, expiresAt)) if expiresAt.isAfter(now) =>
        // TODO: add log/metric to measure the cache get hit rate
        Applicative[F].pure(Some(value))
      case Some(_) =>
        // TODO: add log/metric to measure the cache get miss rate
        store.remove(key)
        Applicative[F].pure(None)
      case None =>
        // TODO: add log/metric to measure the cache get miss rate
        Applicative[F].pure(None)
    }
  }
  override def delete(key: String): Unit = Try(store.remove(key)).toEither match {
    case Right(Some(_)) =>
    case Right(_)       => // TODO: add log/metric to measure cache delete fail rate
    case Left(_)        => // TODO: add log/metric to measure cache delete fail with error
  }
}
