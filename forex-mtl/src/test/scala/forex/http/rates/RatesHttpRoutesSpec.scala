package forex.http.rates
import cats.effect.IO
import forex.helper.MockedObject
import forex.programs.RatesProgram
import forex.programs.rates.Protocol
import forex.programs.rates.errors.{ Error => ProgramError }
import org.http4s.Method.GET
import org.http4s.Status.{ BadGateway, BadRequest, NotFound, Ok, UnprocessableEntity }
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ Request, Response }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext

class RatesHttpRoutesSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures with MockedObject {
  "RatesHttpRoutes" when {
    "GET /rates" should {
      "return 200:OK with valid currency pair" in new Fixture {
        when(ratesProgramMock.get(any[Protocol.GetRatesRequest])).thenReturn(IO(Right(mockedRate)))

        val request: Request[IO]         = Request[IO](GET, uri"/rates?from=CHF&to=CAD")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe Ok
          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture()) { body =>
            body shouldBe """{"from":"CHF","to":"CAD","price":1.2,"timestamp":"2099-12-31T23:59:59Z"}"""
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

      "return 404:NotFound when program returns ExchangeRateNotFound error" in new Fixture {
        when(ratesProgramMock.get(any[Protocol.GetRatesRequest])).thenReturn(
          IO(Left(ProgramError.ExchangeRateNotFound("USD to EUR")))
        )

        val request: Request[IO]         = Request[IO](GET, uri"/rates?from=USD&to=EUR")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe NotFound
          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture()) { body =>
            body shouldBe "Exchange rate USD to EUR is not found"
          }
        }
      }

      "return 502:BadGateway when program returns RateLookupFailed error" in new Fixture {
        when(ratesProgramMock.get(any[Protocol.GetRatesRequest])).thenReturn(
          IO(Left(ProgramError.RateLookupFailed("Service unavailable")))
        )

        val request: Request[IO]         = Request[IO](GET, uri"/rates?from=USD&to=EUR")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe BadGateway
          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture()) { body =>
            body shouldBe "Service unavailable"
          }
        }
      }

      "return 422:UnprocessableEntity when program returns DecodingFailure error" in new Fixture {
        when(ratesProgramMock.get(any[Protocol.GetRatesRequest])).thenReturn(
          IO(Left(ProgramError.DecodingFailure("Failed to parse date: invalid-date")))
        )

        val request: Request[IO]         = Request[IO](GET, uri"/rates?from=USD&to=EUR")
        val responseIO: IO[Response[IO]] = routes.orNotFound.run(request)

        whenReady(responseIO.unsafeToFuture()) { response =>
          response.status shouldBe UnprocessableEntity
          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture()) { body =>
            body shouldBe "Failed to parse date: invalid-date"
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
