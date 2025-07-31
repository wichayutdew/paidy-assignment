package forex.domain.rates

import forex.helper.MockedObject
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RateSpec extends AnyWordSpec with Matchers with MockedObject {
  "Rate" when {
    "fromOneFrameDTO" should {
      "convert RateDTO to Rate" in {
        val rate = Rate.fromOneFrameDTO(mockedRateDTO)

        rate.pair.from shouldBe Currency.USD
        rate.pair.to shouldBe Currency.EUR
        rate.price shouldBe Price(1.2)
        rate.timestamp shouldBe mockedTimestamp
      }
    }
  }
}
