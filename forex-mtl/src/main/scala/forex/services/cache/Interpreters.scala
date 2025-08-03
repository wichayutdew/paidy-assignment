package forex.services.cache

import cats.Applicative
import forex.services.cache.interpreters.{ InMemoryCacheService, RedisService }
import io.lettuce.core.api.sync.RedisCommands
import io.opentelemetry.api.metrics.Meter

//$COVERAGE-OFF$
object Interpreters {
  def redis[F[_]: Applicative](client: RedisCommands[String, String]) = new RedisService[F](client)
  def inMemory[F[_]: Applicative](implicit meter: Meter)              = new InMemoryCacheService[F]
}
//$COVERAGE-ON$
