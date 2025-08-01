package forex.services.rates.interpreters
import cats.Id
import forex.domain.rates.{ Currency, Rate }
import forex.helper.MockedObject
import forex.services.rates.errors
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OneFrameDummySpec extends AnyWordSpec with Matchers with MockedObject {
  "OneFrameDummy" when {
    "get" should {
      "return a dummy rate for any pair" in new Fixture {
        val results: Id[Either[errors.Error, List[Rate]]] = client.get(List(mockedRate.pair), mockedToken)

        results.isRight shouldBe true
        results.toOption.get.head.pair.from shouldBe Currency.CHF
        results.toOption.get.head.pair.to shouldBe Currency.CAD
        results.toOption.get.head.price.value shouldBe BigDecimal(100)
        // Skipping timestamp check as it's too dynamic to test
      }
    }
  }

  trait Fixture {
    val client = new OneFrameDummy[cats.Id]
  }
}
