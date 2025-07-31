package forex.services.rates.interpreters
import cats.Id
import forex.domain.Currency.{ CAD, CHF }
import forex.domain.Rate
import forex.helper.MockedObject
import forex.services.rates.errors
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OneFrameDummySpec extends AnyWordSpec with Matchers with MockedObject {
  "OneFrameDummy" when {
    "get" should {
      "return a dummy rate for any pair" in new Fixture {
        val result: Id[Either[errors.Error, Rate]] = client.get(mockedRate.pair)

        result.isRight shouldBe true
        result.toOption.get.pair.from shouldBe CHF
        result.toOption.get.pair.to shouldBe CAD
        result.toOption.get.price.value shouldBe BigDecimal(100)
        // Skipping timestamp check as it's too dynamic to test
      }
    }
  }

  trait Fixture {
    val client = new OneFrameDummy[cats.Id]
  }
}
