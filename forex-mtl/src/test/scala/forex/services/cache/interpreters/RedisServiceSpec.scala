package forex.services.cache.interpreters
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import forex.domain.core.measurement.metrics.MetricsTag
import io.lettuce.core.SetArgs
import io.lettuce.core.api.sync.RedisCommands
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.{ DoubleHistogram, DoubleHistogramBuilder, LongCounter, LongCounterBuilder, Meter }
import org.mockito.Mockito.{ lenient, verifyNoInteractions }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{ Logger => Slf4jLogger }

import scala.concurrent.duration.{ FiniteDuration, SECONDS }

class RedisServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {
  "RedisService" when {
    "set" should {
      "fill cache successfully" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenReturn("OK")

        redisService.set("key", "value", FiniteDuration(30, SECONDS))

        verify(redisClientMocked).set(any[String], any[String], any[SetArgs])
        verify(counterMocked, times(1)).add(
          1L,
          Attributes
            .builder()
            .put(MetricsTag.STATUS, true.toString)
            .put(MetricsTag.OPERATION, "set")
            .put(MetricsTag.CLIENT, "RedisService")
            .build()
        )
        verifyNoInteractions(loggerMock)
        verifyZeroInteractions(histogramMocked)
      }

      "log warn when set unsuccessfully" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenReturn("invalid")

        redisService.set("key", "value", FiniteDuration(30, SECONDS))

        verify(counterMocked, times(1)).add(
          1L,
          Attributes
            .builder()
            .put(MetricsTag.STATUS, false.toString)
            .put(MetricsTag.OPERATION, "set")
            .put(MetricsTag.CLIENT, "RedisService")
            .build()
        )
        verify(redisClientMocked).set(any[String], any[String], any[SetArgs])
        verify(loggerMock).warn(any[String])
        verifyZeroInteractions(histogramMocked)
      }

      "log error when set error" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenThrow(new Exception("error"))

        redisService.set("key", "value", FiniteDuration(30, SECONDS))

        verify(counterMocked, times(1)).add(
          1L,
          Attributes
            .builder()
            .put(MetricsTag.STATUS, false.toString)
            .put(MetricsTag.OPERATION, "set")
            .put(MetricsTag.CLIENT, "RedisService")
            .build()
        )
        verify(redisClientMocked).set(any[String], any[String], any[SetArgs])
        verify(loggerMock).error(any[String], any[Throwable])
        verifyZeroInteractions(histogramMocked)
      }
    }

    "get" should {
      "retrieve cache successfully" in new Fixture {
        when(redisClientMocked.get(any[String])).thenReturn("result")

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe Some("result")

          verify(redisClientMocked).get(any[String])
          verify(counterMocked, times(2)).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, true.toString)
              .put(MetricsTag.OPERATION, "get")
              .put(MetricsTag.CLIENT, "RedisService")
              .build()
          )
          verifyNoInteractions(loggerMock)
          verify(histogramMocked).record(any[Double], any[Attributes])
        }
      }

      "return None due if result is null" in new Fixture {
        when(redisClientMocked.get(any[String])).thenReturn(null)

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe None

          verify(redisClientMocked).get(any[String])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, true.toString)
              .put(MetricsTag.OPERATION, "get")
              .put(MetricsTag.CLIENT, "RedisService")
              .build()
          )
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, false.toString)
              .put(MetricsTag.OPERATION, "get")
              .put(MetricsTag.CLIENT, "RedisService")
              .build()
          )
          verify(loggerMock).debug(any[String])
          verify(histogramMocked).record(any[Double], any[Attributes])
        }
      }

      "return None due to error while calling redis" in new Fixture {
        when(redisClientMocked.get(any[String])).thenThrow(new Exception("error"))

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe None

          verify(redisClientMocked).get(any[String])
          verify(counterMocked).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, false.toString)
              .put(MetricsTag.OPERATION, "get")
              .put(MetricsTag.CLIENT, "RedisService")
              .build()
          )
          verify(loggerMock).error(any[String], any[Throwable])
          verify(histogramMocked).record(any[Double], any[Attributes])
        }
      }
    }

    "delete" should {
      "delete cache successfully" in new Fixture {
        when(redisClientMocked.del(any[String])).thenReturn(1L)

        redisService.delete("key")

        verify(counterMocked).add(
          1L,
          Attributes
            .builder()
            .put(MetricsTag.STATUS, true.toString)
            .put(MetricsTag.OPERATION, "delete")
            .put(MetricsTag.CLIENT, "RedisService")
            .build()
        )
        verify(redisClientMocked).del(any[String])
        verifyZeroInteractions(loggerMock)
        verifyZeroInteractions(histogramMocked)
      }

      "log debug when delete non existed key" in new Fixture {
        when(redisClientMocked.del(any[String])).thenReturn(0L)

        redisService.delete("key")

        verify(counterMocked).add(
          1L,
          Attributes
            .builder()
            .put(MetricsTag.STATUS, true.toString)
            .put(MetricsTag.OPERATION, "delete")
            .put(MetricsTag.CLIENT, "RedisService")
            .build()
        )
        verify(redisClientMocked).del(any[String])
        verify(loggerMock).debug(any[String])
        verifyZeroInteractions(histogramMocked)
      }

      "log error when delete error" in new Fixture {
        when(redisClientMocked.del(any[String])).thenThrow(new Exception("error"))

        redisService.delete("key")

        verify(counterMocked).add(
          1L,
          Attributes
            .builder()
            .put(MetricsTag.STATUS, false.toString)
            .put(MetricsTag.OPERATION, "delete")
            .put(MetricsTag.CLIENT, "RedisService")
            .build()
        )
        verify(redisClientMocked).del(any[String])
        verify(loggerMock).error(any[String], any[Throwable])
        verifyZeroInteractions(histogramMocked)
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

    val loggerMock: Slf4jLogger = mock[Slf4jLogger]

    lenient().when(loggerMock.isDebugEnabled()).thenReturn(true)
    lenient().when(loggerMock.isWarnEnabled()).thenReturn(true)
    lenient().when(loggerMock.isErrorEnabled()).thenReturn(true)

    val redisClientMocked: RedisCommands[String, String] = mock[RedisCommands[String, String]]
    val redisService: RedisService[IO]                   = new RedisService[IO](redisClientMocked) {
      override lazy val logger: Logger = Logger(loggerMock)
    }
  }
}
