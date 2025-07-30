package forex.programs.rates
import org.scalatest.wordspec.AnyWordSpec
import forex.services.rates.errors.{ Error => RatesServiceError }
import forex.programs.rates.errors.{ Error => ProgramError }
import org.scalatest.matchers.should.Matchers

class errorsTest extends AnyWordSpec with Matchers {
  "errors" when {
    "toProgramError" should {
      "convert ServiceError correctly" in {
        val ratesServiceError = RatesServiceError.OneFrameLookupFailed("test error")

        val result = errors.toProgramError(ratesServiceError)

        result shouldBe ProgramError.RateLookupFailed("test error")
      }
    }
  }
}
