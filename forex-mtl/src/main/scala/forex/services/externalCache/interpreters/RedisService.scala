package forex.services.externalCache.interpreters

import cats.Applicative
import forex.services.externalCache.Algebra
import forex.services.externalCache.errors._
import io.lettuce.core.SetArgs
import io.lettuce.core.api.sync.RedisCommands

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class RedisService[F[_]: Applicative](client: RedisCommands[String, String]) extends Algebra[F] {
  override def set(key: String, value: String, ttl: FiniteDuration): F[Error Either String] =
    Try(client.set(key, value, SetArgs.Builder.px(ttl.toMillis))).toEither match {
      case Right("OK") => Applicative[F].pure(Right("Value set successfully"))
      case Right(_)    => Applicative[F].pure(Left(Error.CachePutFailed(s"Failed to set value for key '$key'")))
      case Left(error) => Applicative[F].pure(Left(Error.CachePutFailed(error.getMessage)))
    }

  override def get(key: String): F[Error Either String] = Try(client.get(key)).toEither match {
    case Right(value) if value != null => Applicative[F].pure(Right(value))
    case Right(_)                      => Applicative[F].pure(Left(Error.CacheExpired(s"cache '$key' expired")))
    case Left(error)                   => Applicative[F].pure(Left(Error.CacheGetFailed(error.getMessage)))
  }
}
