package forex.domain.rates

import forex.helper.MockedObject
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.ZoneId

class RateSpec extends AnyWordSpec with Matchers with MockedObject {
  "Rate" when {
    "fromOneFrameDTO" should {
      "convert RateDTO to Rate with server timezone" in {
        val rate = Rate.fromOneFrameDTO(mockedRateDTO)

        rate.pair.from shouldBe Currency.USD
        rate.pair.to shouldBe Currency.EUR
        rate.price shouldBe Price(1.2)
        rate.timestamp shouldBe mockedTimestamp.copy(
          mockedTimestamp.value.atZoneSameInstant(ZoneId.systemDefault()).toOffsetDateTime
        )
      }
    }
  }
}
