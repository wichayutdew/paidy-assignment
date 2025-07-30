package forex.http.rates
import cats.effect.IO
import forex.domain.Currency.{ EUR, USD }
import forex.domain.{ Price, Rate, Timestamp }
import forex.programs.RatesProgram
import forex.programs.rates.Protocol
import org.http4s.Method.GET
import org.http4s.Status.{ BadRequest, Ok }
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Request, Response }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class RatesHttpRoutesSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {
  "RatesHttpRoutes" when {
    "GET /rates" should {
      "return 200:OK with valid currency pair" in new Fixture {
        when(ratesProgramMock.get(any[Protocol.GetRatesRequest])).thenReturn(
          IO(
            Right(
              Rate(
                pair = Rate.Pair(USD, EUR),
                price = Price(BigDecimal(1.2)),
                timestamp = Timestamp.now
              )
            )
          )
        )

        val request: Request[IO]         = Request[IO](GET, uri"/rates?from=USD&to=EUR")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe Ok // For automatic derivation of Circe decoders
          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture()) { body =>
            body should startWith("""{"from":"USD","to":"EUR","price":1.2""")
          }
        }
      }

      "return 400:BadRequest with invalid currency pair" in new Fixture {
        val request: Request[IO]         = Request[IO](GET, uri"/rates?from=&to=EUR")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe BadRequest
          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture()) { body =>
            body shouldBe "Invalid `from` parameter: Empty currency: Currency parameter should not be empty"
          }
        }
      }
    }
  }

  trait Fixture {
    implicit val ec: ExecutionContext = ExecutionContext.global
    val ratesProgramMock              = mock[RatesProgram[IO]]
    val routes                        = new RatesHttpRoutes[IO](ratesProgramMock).routes
  }
}
