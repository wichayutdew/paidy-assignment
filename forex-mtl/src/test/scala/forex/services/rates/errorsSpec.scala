package forex.services.rates
import forex.services.rates.errors.{ Error => RatesServiceError }
import forex.services.vault.errors.{ Error => VaultError }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class errorsSpec extends AnyWordSpec with Matchers {
  "errors" when {
    "toRatesError" should {
      "convert ServiceError correctly" in {
        val oneFrameError = VaultError.SecretLookupFailed("secret not found")
        errors.toRatesError(oneFrameError) shouldBe RatesServiceError.InvalidToken("secret not found")
      }
    }
  }
}
