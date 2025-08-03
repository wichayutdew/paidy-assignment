package forex.domain.core.measurement.metrics
import com.typesafe.scalalogging.Logger
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.LongCounter
import org.mockito.Mockito.lenient
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{ Logger => Slf4jLogger }

class EventCounterSpec extends AnyWordSpec with MockitoSugar with Matchers {
  "EventCounter" when {
    "record" should {
      "increment the counter and debug log" in new Fixture {
        eventCounter.record()

        verify(counterMocked).add(
          1L,
          Attributes
            .builder()
            .put(MetricsTag.CLIENT, "testClient")
            .build()
        )
        verify(loggerMock).debug(any[String])
      }
    }
    "buildAttributes" should {
      "return attributes with client and additional tags" in new Fixture {
        val additionalTags: Map[String, String] = Map("tag1" -> "value1", "tag2" -> "value2")

        eventCounter.buildAttributes(additionalTags) shouldBe Attributes
          .builder()
          .put(MetricsTag.CLIENT, "testClient")
          .put("tag1", "value1")
          .put("tag2", "value2")
          .build()

      }
    }
  }
  trait Fixture {
    val counterMocked: LongCounter = mock[LongCounter]

    val loggerMock: Slf4jLogger = mock[Slf4jLogger]
    lenient().when(loggerMock.isDebugEnabled()).thenReturn(true)

    val eventCounter: EventCounter = new EventCounter(counterMocked, "testClient") {
      override lazy val logger: Logger = Logger(loggerMock)
    }
  }
}
