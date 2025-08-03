package forex.services.rates.interpreters
import cats.effect.{ IO, Resource }
import com.typesafe.scalalogging.Logger
import forex.domain.core.measurement.metrics.MetricsTag
import forex.domain.rates.Rate
import forex.helper.MockedObject
import forex.services.rates.errors.Error.{ DecodingFailure, OneFrameLookupFailed }
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.{ DoubleHistogram, DoubleHistogramBuilder, LongCounter, LongCounterBuilder, Meter }
import org.http4s.client.Client
import org.http4s.{ Request, Response, Status }
import org.mockito.Mockito.lenient
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{ Logger => Slf4jLogger }

class OneFrameServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with MockedObject with ScalaFutures {

  "OneFrameService" when {
    "get" should {
      "return a rate for a valid pair" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.Ok).withEntity(
          """[{"from":"USD","to":"EUR","bid":1.2,"ask":1.3,"price":1.2,"time_stamp":"2099-12-31T23:59:59Z"}]"""
        )
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service.get(List(mockedRate.pair), mockedToken).unsafeToFuture()) { response =>
          response shouldBe Right(List(Rate.fromOneFrameDTO(mockedRateDTO)))

          verify(mockedClient).run(any[Request[IO]])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, true.toString)
              .put(MetricsTag.OPERATION, "rates")
              .put(MetricsTag.CLIENT, "OneFrameService")
              .build()
          )
          verify(histogramMocked).record(any[Double], any[Attributes])
          verifyZeroInteractions(loggerMock)
        }
      }

      "return OneFrameLookupFailed if OneFrame returns an error" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.BadGateway).withEntity("Error")
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service.get(List(mockedRate.pair), mockedToken).unsafeToFuture()) { response =>
          response shouldBe Left(OneFrameLookupFailed("Error"))

          verify(mockedClient).run(any[Request[IO]])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, false.toString)
              .put(MetricsTag.OPERATION, "rates")
              .put(MetricsTag.CLIENT, "OneFrameService")
              .build()
          )
          verify(histogramMocked).record(any[Double], any[Attributes])
          verify(loggerMock).error(any[String])
        }
      }

      "return DecodingFailure if response cannot be decoded" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.Ok).withEntity("""[{"from":"USD}]""")
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service.get(List(mockedRate.pair), mockedToken).unsafeToFuture()) { response =>
          response.isLeft shouldBe true
          response.left.toOption.get shouldBe a[DecodingFailure]

          verify(mockedClient).run(any[Request[IO]])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, true.toString)
              .put(MetricsTag.OPERATION, "rates")
              .put(MetricsTag.CLIENT, "OneFrameService")
              .build()
          )
          verify(histogramMocked).record(any[Double], any[Attributes])
          verify(loggerMock).error(any[String], any[Throwable])
        }
      }
    }
  }

  trait Fixture {
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

    val mockedClient: Client[IO] = mock[Client[IO]]

    val loggerMock: Slf4jLogger = mock[Slf4jLogger]
    lenient().when(loggerMock.isWarnEnabled()).thenReturn(true)
    lenient().when(loggerMock.isErrorEnabled()).thenReturn(true)

    val service: OneFrameService[IO] = new OneFrameService[IO](
      mockedClient,
      mockedApplicationConfig.client.oneFrame
    ) {
      override lazy val logger: Logger = Logger(loggerMock)
    }
  }
}
