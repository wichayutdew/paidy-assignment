package forex.domain
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CurrencySpec extends AnyWordSpec with Matchers {
  "Currency" when {
    "fromString" should {
      "return a valid Currency for supported currencies" in {
        Currency.fromString("USD") shouldBe Right(Currency.USD)
      }

      "return an error for unsupported currencies" in {
        Currency.fromString("XYZ") shouldBe Left(CurrencyError.Unsupported("XYZ"))
      }

      "return an error for empty strings" in {
        Currency.fromString("") shouldBe Left(CurrencyError.Empty)
      }

      "return an error for blank strings" in {
        Currency.fromString(" ") shouldBe Left(CurrencyError.Empty)
      }
    }
  }
}
