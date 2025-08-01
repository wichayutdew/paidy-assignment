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
      case Right(_) => // TODO: add log/metric to measure cache put fail rate
      case Left(_)  => // TODO: add log/metric to measure cache put fail with error
      case _        =>
    }

  override def get(key: String): F[Option[String]] = Try(client.get(key)).toEither match {
    case Right(value) if value != null => Applicative[F].pure(Some(value))
    case Right(_)                      =>
      // TODO: add log/metric to measure the cache expired rate
      Applicative[F].pure(None)
    case Left(_) =>
      // TODO: add log/metric to measure the cache get Error
      Applicative[F].pure(None)
  }
}
