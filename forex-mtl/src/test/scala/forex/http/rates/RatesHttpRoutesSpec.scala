package forex.http.rates
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import forex.domain.core.measurement.metrics.MetricsTag
import forex.helper.MockedObject
import forex.programs.RatesProgram
import forex.programs.rates.Protocol
import forex.programs.rates.errors.{ Error => ProgramError }
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.{ DoubleHistogram, DoubleHistogramBuilder, LongCounter, LongCounterBuilder, Meter }
import org.http4s.Method.GET
import org.http4s.Status.{ BadGateway, BadRequest, NotFound, Ok, UnprocessableEntity }
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{ HttpRoutes, Request, Response }
import org.mockito.Mockito.lenient
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{ Logger => Slf4jLogger }

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

          verify(ratesProgramMock).get(any[Protocol.GetRatesRequest])
          verifyZeroInteractions(loggerMock)
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, true.toString)
              .put(MetricsTag.OPERATION, "rates")
              .put(MetricsTag.CLIENT, "RatesHttpRoutes")
              .build()
          )
          verify(histogramMocked).record(any[Double], any[Attributes])

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

          verifyZeroInteractions(ratesProgramMock)
          verify(loggerMock).error(any[String])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, false.toString)
              .put(MetricsTag.OPERATION, "rates")
              .put(MetricsTag.CLIENT, "RatesHttpRoutes")
              .build()
          )
          verify(histogramMocked).record(any[Double], any[Attributes])

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

          verify(ratesProgramMock).get(any[Protocol.GetRatesRequest])
          verify(loggerMock).error(any[String], any[Throwable])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, false.toString)
              .put(MetricsTag.OPERATION, "rates")
              .put(MetricsTag.CLIENT, "RatesHttpRoutes")
              .build()
          )
          verify(histogramMocked).record(any[Double], any[Attributes])

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

          verify(ratesProgramMock).get(any[Protocol.GetRatesRequest])
          verify(loggerMock).error(any[String], any[Throwable])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, false.toString)
              .put(MetricsTag.OPERATION, "rates")
              .put(MetricsTag.CLIENT, "RatesHttpRoutes")
              .build()
          )
          verify(histogramMocked).record(any[Double], any[Attributes])

          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture()) { body =>
            body shouldBe "Unable to lookup exchange rate due to external service failure"
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

          verify(ratesProgramMock).get(any[Protocol.GetRatesRequest])
          verify(loggerMock).error(any[String], any[Throwable])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, false.toString)
              .put(MetricsTag.OPERATION, "rates")
              .put(MetricsTag.CLIENT, "RatesHttpRoutes")
              .build()
          )
          verify(histogramMocked).record(any[Double], any[Attributes])

          val bodyIO: IO[String] = response.as[String]
          whenReady(bodyIO.unsafeToFuture()) { body =>
            body shouldBe "Unable to decode response from external service"
          }
        }
      }
    }
  }

  trait Fixture {
    implicit val ec: ExecutionContext = ExecutionContext.global

    implicit val meterMocked: Meter                          = mock[Meter]
    private val longCounterBuilderMocked: LongCounterBuilder = mock[LongCounterBuilder]
    when(meterMocked.counterBuilder(any[String])).thenReturn(longCounterBuilderMocked)
    when(longCounterBuilderMocked.setDescription(any[String])).thenReturn(longCounterBuilderMocked)
    val counterMocked: LongCounter = mock[LongCounter]
    when(longCounterBuilderMocked.build()).thenReturn(counterMocked)

    private val doubleHistogramBuilderMocked: DoubleHistogramBuilder = mock[DoubleHistogramBuilder]
    when(meterMocked.histogramBuilder(any[String])).thenReturn(doubleHistogramBuilderMocked)
    when(doubleHistogramBuilderMocked.setDescription(any[String])).thenReturn(doubleHistogramBuilderMocked)
    when(doubleHistogramBuilderMocked.setUnit(any[String])).thenReturn(doubleHistogramBuilderMocked)
    val histogramMocked: DoubleHistogram = mock[DoubleHistogram]
    when(doubleHistogramBuilderMocked.build()).thenReturn(histogramMocked)

    val ratesProgramMock: RatesProgram[IO] = mock[RatesProgram[IO]]

    val loggerMock: Slf4jLogger = mock[Slf4jLogger]

    lenient().when(loggerMock.isErrorEnabled()).thenReturn(true)

    val routes: HttpRoutes[IO] = new RatesHttpRoutes[IO](ratesProgramMock) {
      override lazy val logger: Logger = Logger(loggerMock)
    }.routes
  }
}
