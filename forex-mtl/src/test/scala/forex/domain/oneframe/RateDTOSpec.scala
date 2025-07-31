package forex.domain.oneframe
import forex.helper.MockedObject
import io.circe.parser.decode
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RateDTOSpec extends AnyWordSpec with Matchers with MockedObject {
  "RateDTO" when {
    "decode" should {
      "return RateDTO instance with valid JSON" in {
        val json = s"""{
                      |        "from": "USD",
                      |        "to": "EUR",
                      |        "bid": 1.2,
                      |        "ask": 1.3,
                      |        "price": 1.2,
                      |        "time_stamp": "2099-12-31T23:59:59.0Z"
                      |    }""".stripMargin

        decode[RateDTO](json) shouldBe Right(mockedRateDTO)
      }

      "fail to decode invalid RateDTO json" in {
        val invalidJson = s"""{
                      |        "from": "USD",
                      |    }""".stripMargin

        val decoded = decode[RateDTO](invalidJson)
        decoded.isLeft shouldBe true

      }
    }
  }
}
