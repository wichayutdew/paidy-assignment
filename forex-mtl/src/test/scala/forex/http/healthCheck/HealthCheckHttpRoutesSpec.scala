package forex.http.healthCheck
import cats.effect.IO
import org.http4s.Method.GET
import org.http4s.Status.Ok
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ HttpRoutes, Request, Response }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HealthCheckHttpRoutesSpec extends AnyWordSpec with ScalaFutures with Matchers {

  "HealthCheckHttpRoutes" when {
    "GET /healths" should {
      "return 200:OK" in new Fixture {
        val request: Request[IO]         = Request[IO](GET, uri"/health")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe Ok
          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture())(body => body shouldBe "ready")
        }
      }
    }
  }

  trait Fixture {
    val routes: HttpRoutes[IO] = new HealthCheckHttpRoutes[IO].routes
  }
}
