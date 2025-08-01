package forex.domain.rates

import io.circe.DecodingFailure
import io.circe.parser.decode
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

    "getAllPairs" should {
      "return all pairs of currencies excluding self-pairs" in {
        val pairs = Currency.getAllPairs
        pairs.size shouldBe (Currency.values.size * (Currency.values.size - 1))
        pairs.forall(pair => pair.from != pair.to) shouldBe true
      }
    }

    "decode" should {
      "decode valid currency JSON strings" in {
        decode[Currency]("\"USD\"") shouldBe Right(Currency.USD)
        decode[Currency]("\"EUR\"") shouldBe Right(Currency.EUR)
      }

      "fail to decode invalid currency JSON strings" in {
        decode[Currency]("\"XYZ\"") shouldBe Left(DecodingFailure("Unsupported currency code: XYZ", List()))
        decode[Currency]("\"\"") shouldBe Left(DecodingFailure("Currency code is empty", List()))
      }
    }
  }
}
