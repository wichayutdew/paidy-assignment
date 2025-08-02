package forex.http.healthCheck

import cats.effect.Sync
import forex.domain.rates.Constant.PATH
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class HealthCheckHttpRoutes[F[_]: Sync] extends Http4sDsl[F] {
  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root => Ok("ready") }
  val routes: HttpRoutes[F]             = Router(PATH.HEALTH_CHECK -> httpRoutes)
}
