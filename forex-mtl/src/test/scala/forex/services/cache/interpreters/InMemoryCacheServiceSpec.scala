package forex.services.cache.interpreters
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import forex.domain.core.measurement.metrics.MetricsTag
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.{LongCounter, LongCounterBuilder, Meter}
import org.mockito.Mockito.lenient
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{Logger => Slf4jLogger}

import scala.concurrent.duration.{FiniteDuration, SECONDS}

class InMemoryCacheServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {
  "InMemoryCacheService" when {
    "set" should {
      "set cache successfully" in new Fixture {
        service.set("key", "value", FiniteDuration(10, SECONDS))
        verifyZeroInteractions(loggerMock)
        verifyZeroInteractions(counterMocked)
      }

      "replace cache successfully" in new Fixture {
        service.set("key", "value", FiniteDuration(10, SECONDS))
        service.set("key", "value2", FiniteDuration(10, SECONDS))
        verify(loggerMock).debug(any[String])
        verifyZeroInteractions(counterMocked)
      }
    }

    "get" should {
      "return value if key exists and send cache hit metric" in new Fixture {
        service.set("key", "value", FiniteDuration(10, SECONDS))
        whenReady(service.get("key").unsafeToFuture()) { result =>
          result shouldBe Some("value")

          verifyZeroInteractions(loggerMock)
          verify(counterMocked, times(1)).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, true.toString)
              .put(MetricsTag.OPERATION, "get")
              .put(MetricsTag.CLIENT, "InMemoryCacheService")
              .build()
          )
        }
      }

      "return None if cache is expired and send cache miss metric" in new Fixture {
        service.set("key", "value", FiniteDuration(0, SECONDS))
        whenReady(service.get("key").unsafeToFuture()) { result =>
          result shouldBe None

          verify(loggerMock).debug(any[String])
          verify(counterMocked, times(1)).add(
            1L,
            Attributes
              .builder()
              .put(MetricsTag.STATUS, false.toString)
              .put(MetricsTag.OPERATION, "get")
              .put(MetricsTag.CLIENT, "InMemoryCacheService")
              .build()
          )
        }
      }

      "return None if key is not exists" in new Fixture {
        whenReady(service.get("key").unsafeToFuture()) { result =>
          result shouldBe None
          verify(loggerMock).debug(any[String])
          verifyZeroInteractions(counterMocked)
        }
      }
    }
    "delete" should {
      "delete cache success and send success set metric" in new Fixture {
        service.set("key", "value", FiniteDuration(10, SECONDS))
        service.delete("key")
        verifyZeroInteractions(loggerMock)
        verifyZeroInteractions(counterMocked)
      }
      "delete cache failed and send failure set metric" in new Fixture {
        service.delete("key")
        verify(loggerMock).debug(any[String])
        verifyZeroInteractions(counterMocked)
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

    val loggerMock: Slf4jLogger = mock[Slf4jLogger]
    lenient().when(loggerMock.isDebugEnabled).thenReturn(true)

    val service: InMemoryCacheService[IO] = new InMemoryCacheService[IO] {
      override lazy val logger: Logger = Logger(loggerMock)
    }
  }
}
