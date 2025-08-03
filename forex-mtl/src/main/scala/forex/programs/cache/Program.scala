package forex.programs.cache

import cats.Applicative
import forex.domain.core.Constant.PATH
import forex.services.cache.interpreters.{ InMemoryCacheService, RedisService }

class Program[F[_]: Applicative](
    redisService: RedisService[F],
    inMemoryCacheService: InMemoryCacheService[F]
) extends Algebra[F] {
  override def delete(key: String, service: String): F[Unit] = service match {
    case str if str == PATH.REDIS  => Applicative[F].pure(redisService.delete(key))
    case str if str == PATH.MEMORY => Applicative[F].pure(inMemoryCacheService.delete(key))
    case _                         => Applicative[F].pure(())
  }
}

object Program {

  def apply[F[_]: Applicative](
      redisService: RedisService[F],
      inMemoryCacheService: InMemoryCacheService[F]
  ): Program[F] = new Program[F](
    redisService,
    inMemoryCacheService
  )

}
