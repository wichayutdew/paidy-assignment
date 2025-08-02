package forex.services.cache.interpreters
import cats.Applicative
import forex.domain.core.measurement.logging.{ AppLogger, ErrorLog, WarnLog }
import forex.services.cache.Algebra

import java.time.Instant
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class InMemoryCacheService[F[_]: Applicative] extends Algebra[F] with AppLogger {
  private case class CacheEntry(value: String, expiresAt: Instant)
  private val store = TrieMap.empty[String, CacheEntry]

  override def set(key: String, value: String, ttl: FiniteDuration): Unit = {
    val expiresAt = Instant.now().plusMillis(ttl.toMillis)
    Try(store.put(key, CacheEntry(value, expiresAt))).toEither match {
      case Right(Some(_)) =>
      case Right(None)    =>
        // TODO: add metric to measure cache put fail rate
        logger.log(WarnLog(message = s"[In-Mem]Cache with key :$key put failed"))
      case Left(error) =>
        // TODO: add metric to measure cache put fail with error
        logger.log(ErrorLog(message = s"[In-Mem]Cache with key :$key put failed", cause = Some(error)))
    }
  }

  override def get(key: String): F[Option[String]] = {
    val now = Instant.now()
    store.get(key) match {
      case Some(CacheEntry(value, expiresAt)) if expiresAt.isAfter(now) =>
        // TODO: add metric to measure the cache get hit rate
        Applicative[F].pure(Some(value))
      case Some(_) =>
        // TODO: add metric to measure the cache get miss rate
        logger.log(WarnLog(message = s"[In-Mem]Cache expired for key $key"))
        store.remove(key)
        Applicative[F].pure(None)
      case None =>
        // TODO: add metric to measure the cache get miss rate
        logger.log(WarnLog(message = s"[In-Mem]Cache miss for key: $key"))
        Applicative[F].pure(None)
    }
  }
  override def delete(key: String): Unit = Try(store.remove(key)).toEither match {
    case Right(Some(_)) =>
    case Right(None)    =>
      // TODO: add metric to measure cache delete fail rate
      logger.log(WarnLog(message = s"[In-Mem]Cache with key :$key delete failed"))
    case Left(error) =>
      // TODO: add metric to measure cache delete fail with error
      logger.log(ErrorLog(message = s"[In-Mem]Cache with key :$key delete failed", cause = Some(error)))
  }
}
