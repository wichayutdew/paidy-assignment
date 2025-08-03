package forex.services.cache.interpreters

import cats.Applicative
import forex.domain.core.measurement.logging.{ AppLogger, DebugLog, ErrorLog, WarnLog }
import forex.domain.core.measurement.metrics.{ EventCounter, MetricsTag }
import forex.services.cache.Algebra
import io.lettuce.core.SetArgs
import io.lettuce.core.api.sync.RedisCommands
import io.opentelemetry.api.metrics.Meter

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class RedisService[F[_]: Applicative](client: RedisCommands[String, String])(implicit meter: Meter)
    extends Algebra[F]
    with AppLogger {
  private val successRateCounter: EventCounter = EventCounter("client.success", "RedisService")
  private val hitRateCounter: EventCounter     = EventCounter("client.cache", "RedisService")

  override def set(key: String, value: String, ttl: FiniteDuration): Unit =
    Try(client.set(key, value, SetArgs.Builder.px(ttl.toMillis))).toEither match {
      case Right(result) if result == "OK" =>
        successRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "set"))
      case Right(result) =>
        successRateCounter.record(Map(MetricsTag.STATUS -> false.toString, MetricsTag.OPERATION -> "set"))
        logger.log(WarnLog(message = s"[Redis] Cache with key :$key put failed due to unexpected result: $result"))
      case Left(error) =>
        successRateCounter.record(Map(MetricsTag.STATUS -> false.toString, MetricsTag.OPERATION -> "set"))
        logger.log(ErrorLog(message = s"[Redis]Cache with key :$key put failed", cause = Some(error)))
    }

  override def get(key: String): F[Option[String]] = Try(client.get(key)).toEither match {
    case Right(value) if value != null =>
      successRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "get"))
      hitRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "get"))
      Applicative[F].pure(Some(value))
    case Right(_) =>
      successRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "get"))
      hitRateCounter.record(Map(MetricsTag.STATUS -> false.toString, MetricsTag.OPERATION -> "get"))
      logger.log(DebugLog(message = s"[Redis]Cache expired for key $key"))
      Applicative[F].pure(None)
    case Left(error) =>
      successRateCounter.record(Map(MetricsTag.STATUS -> false.toString, MetricsTag.OPERATION -> "get"))
      logger.log(ErrorLog(message = s"[Redis]Cache miss for key: $key", Some(error)))
      Applicative[F].pure(None)
  }
  override def delete(key: String): Unit = Try(client.del(key)).toEither match {
    case Right(count) if count > 0 =>
      successRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "delete"))
    case Right(_) =>
      successRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "delete"))
      logger.log(DebugLog(message = s"[Redis]No cache entry found for key: $key"))
    case Left(error) =>
      successRateCounter.record(Map(MetricsTag.STATUS -> false.toString, MetricsTag.OPERATION -> "delete"))
      logger.log(ErrorLog(message = s"[Redis]Cache with key :$key delete failed", cause = Some(error)))
  }
}
