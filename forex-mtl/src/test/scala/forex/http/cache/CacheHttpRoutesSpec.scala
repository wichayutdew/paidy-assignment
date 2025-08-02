package forex.http.cache
import cats.effect.IO
import forex.programs.CacheProgram
import org.http4s.Method.DELETE
import org.http4s.Status.Ok
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ HttpRoutes, Request, Response }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CacheHttpRoutesSpec extends AnyWordSpec with ScalaFutures with Matchers with MockitoSugar {
  "CacheHttpRoutes" when {
    "DELETE /cache/:service/:key" should {
      "return 200:OK when delete REDIS cache" in new Fixture {
        val request: Request[IO]         = Request[IO](DELETE, uri"/cache/redis/key")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe Ok
          verify(cacheProgramMock).delete("key", "redis")
        }
      }

      "return 200:OK when delete MEMORY cache" in new Fixture {
        val request: Request[IO]         = Request[IO](DELETE, uri"/cache/memory/key")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe Ok
          verify(cacheProgramMock).delete("key", "memory")
        }
      }
    }
  }
  trait Fixture {
    val cacheProgramMock: CacheProgram[IO] = mock[CacheProgram[IO]]
    val routes: HttpRoutes[IO]             = new CacheHttpRoutes[IO](cacheProgramMock).routes
  }
}
