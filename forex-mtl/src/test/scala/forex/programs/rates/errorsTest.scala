package forex.programs.rates
import org.scalatest.wordspec.AnyWordSpec
import forex.services.rates.errors.{ Error => RatesServiceError }
import forex.programs.rates.errors.{ Error => ProgramError }
import org.scalatest.matchers.should.Matchers

class errorsTest extends AnyWordSpec with Matchers {
  "errors" when {
    "toProgramError" should {
      "convert ServiceError correctly" in {
        val oneFrameError = RatesServiceError.OneFrameLookupFailed("Service Unavailable")
        errors.toProgramError(oneFrameError) shouldBe ProgramError.RateLookupFailed("Service Unavailable")

        val exchangeRateNotFound = RatesServiceError.ExchangeRateNotFound("USD to EUR")
        errors.toProgramError(exchangeRateNotFound) shouldBe ProgramError.ExchangeRateNotFound("USD to EUR")

        val decodingFailure = RatesServiceError.DecodingFailure("Unsupported currency code")
        errors.toProgramError(decodingFailure) shouldBe ProgramError.DecodingFailure("Unsupported currency code")
      }
    }
  }
}
