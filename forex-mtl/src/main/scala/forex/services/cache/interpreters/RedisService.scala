package forex.services.cache.interpreters

import cats.Applicative
import forex.domain.core.measurement.logging.{ AppLogger, ErrorLog, WarnLog }
import forex.services.cache.Algebra
import io.lettuce.core.SetArgs
import io.lettuce.core.api.sync.RedisCommands

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class RedisService[F[_]: Applicative](client: RedisCommands[String, String]) extends Algebra[F] with AppLogger {
  override def set(key: String, value: String, ttl: FiniteDuration): Unit =
    Try(client.set(key, value, SetArgs.Builder.px(ttl.toMillis))).toEither match {
      case Right(result) if result == "OK" =>
      case Right(result)                   =>
        // TODO: add metric to measure cache put fail rate
        logger.log(WarnLog(message = s"[Redis] Cache with key :$key put failed due to unexpected result: $result"))
      case Left(error) =>
        // TODO: add metric to measure cache put fail with error
        logger.log(ErrorLog(message = s"[Redis]Cache with key :$key put failed", cause = Some(error)))
    }

  override def get(key: String): F[Option[String]] = Try(client.get(key)).toEither match {
    case Right(value) if value != null => Applicative[F].pure(Some(value))
    case Right(_)                      =>
      // TODO: add metric to measure the cache get miss rate
      logger.log(WarnLog(message = s"[Redis]Cache expired for key $key"))
      Applicative[F].pure(None)
    case Left(error) =>
      // TODO: add metric to measure the cache get miss rate
      logger.log(ErrorLog(message = s"[Redis]Cache miss for key: $key", Some(error)))
      Applicative[F].pure(None)
  }
  override def delete(key: String): Unit = Try(client.del(key)).toEither match {
    case Right(count) if count > 0 =>
    case Right(_)                  =>
      // TODO: add metric to measure cache delete fail rate
      logger.log(WarnLog(message = s"[Redis]Cache with key :$key delete failed"))
    case Left(error) =>
      // TODO: add metric to measure cache delete fail with error
      logger.log(ErrorLog(message = s"[Redis]Cache with key :$key delete failed", cause = Some(error)))
  }
}
