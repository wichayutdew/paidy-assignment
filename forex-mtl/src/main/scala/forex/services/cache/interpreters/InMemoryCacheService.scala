package forex.services.cache.interpreters
import cats.Applicative
import forex.domain.core.measurement.logging.{ AppLogger, DebugLog }
import forex.domain.core.measurement.metrics.{ EventCounter, MetricsTag }
import forex.services.cache.Algebra
import io.opentelemetry.api.metrics.Meter

import java.time.Instant
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration

class InMemoryCacheService[F[_]: Applicative](implicit meter: Meter) extends Algebra[F] with AppLogger {
  private case class CacheEntry(value: String, expiresAt: Instant)
  private val store = TrieMap.empty[String, CacheEntry]

  private val hitRateCounter: EventCounter = EventCounter("client.cache", "InMemoryCacheService")

  override def set(key: String, value: String, ttl: FiniteDuration): Unit = {
    val expiresAt = Instant.now().plusMillis(ttl.toMillis)
    store.put(key, CacheEntry(value, expiresAt)) match {
      case Some(oldCache) =>
        logger.log(DebugLog(message = s"[In-Mem]Cache with key :$key exists, replacing old value: ${oldCache.value}"))
      case None =>
    }
  }

  override def get(key: String): F[Option[String]] = {
    val now = Instant.now()
    store.get(key) match {
      case Some(CacheEntry(value, expiresAt)) if expiresAt.isAfter(now) =>
        hitRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "get"))
        Applicative[F].pure(Some(value))
      case Some(_) =>
        hitRateCounter.record(Map(MetricsTag.STATUS -> false.toString, MetricsTag.OPERATION -> "get"))
        logger.log(DebugLog(message = s"[In-Mem]Cache expired for key $key"))
        store.remove(key)
        Applicative[F].pure(None)
      case None =>
        logger.log(DebugLog(message = s"[In-Mem]No cache entry found for key: $key"))
        Applicative[F].pure(None)
    }
  }
  override def delete(key: String): Unit = store.remove(key) match {
    case Some(_) =>
    case None    =>
      logger.log(DebugLog(message = s"[In-Mem]No cache entry found for key: $key"))
  }
}
