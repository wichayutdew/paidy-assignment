package forex.http.cache

import cats.effect.Sync
import forex.domain.core.Constant.PATH
import forex.programs.CacheProgram
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class CacheHttpRoutes[F[_]: Sync](program: CacheProgram[F]) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / service / key =>
    program.delete(key, service)
    Ok()
  }

  val routes: HttpRoutes[F] = Router(
    PATH.CACHE -> httpRoutes
  )

}
