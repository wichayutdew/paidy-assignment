package forex.services.cache

import cats.Applicative
import forex.services.cache.interpreters.RedisService
import io.lettuce.core.api.sync.RedisCommands

//$COVERAGE-OFF$
object Interpreters {
  def redis[F[_]: Applicative](client: RedisCommands[String, String]) = new RedisService[F](client)
}
//$COVERAGE-ON$
