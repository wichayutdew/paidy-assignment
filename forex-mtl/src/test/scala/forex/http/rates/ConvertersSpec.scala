package forex.http.rates
import forex.domain.Currency.{ CAD, CHF }
import forex.domain.Price
import forex.helper.MockedObject
import forex.http.rates.Converters.GetApiResponseOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConvertersSpec extends AnyWordSpec with Matchers with MockedObject {
  "Converters" when {
    "asGetApiResponse" should {
      "convert Rate to GetApiResponse" in {
        val result = mockedRate.asGetApiResponse

        result.from shouldBe CHF
        result.to shouldBe CAD
        result.price shouldBe Price(BigDecimal(1.2))
        result.timestamp shouldBe mockedTimestamp
      }
    }
  }
}
