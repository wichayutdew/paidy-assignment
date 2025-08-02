package forex.services.cache.interpreters

import cats.Applicative
import forex.services.cache.Algebra
import io.lettuce.core.SetArgs
import io.lettuce.core.api.sync.RedisCommands

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class RedisService[F[_]: Applicative](client: RedisCommands[String, String]) extends Algebra[F] {
  override def set(key: String, value: String, ttl: FiniteDuration): Unit =
    Try(client.set(key, value, SetArgs.Builder.px(ttl.toMillis))).toEither match {
      case Right(result) if result == "OK" =>
      case Right(_)                        => // TODO: add log/metric to measure cache put fail rate
      case Left(_)                         => // TODO: add log/metric to measure cache put fail with error
    }

  override def get(key: String): F[Option[String]] = Try(client.get(key)).toEither match {
    case Right(value) if value != null => Applicative[F].pure(Some(value))
    case Right(_)                      =>
      // TODO: add log/metric to measure the cache get miss rate
      Applicative[F].pure(None)
    case Left(_) =>
      // TODO: add log/metric to measure the cache get miss rate
      Applicative[F].pure(None)
  }
  override def delete(key: String): Unit = Try(client.del(key)).toEither match {
    case Right(count) if count > 0 =>
    case Right(_)                  => // TODO: add log/metric to measure cache delete fail rate if count == 0
    case Left(_)                   => // TODO: add log/metric to measure cache delete fail with error
  }
}
