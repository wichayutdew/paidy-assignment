package forex.programs.rates
import cats.Id
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.services.RatesService
import forex.services.rates.errors.Error.OneFrameLookupFailed
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProgramSpec extends AnyWordSpec with Matchers with MockitoSugar {
  "Program" when {
    "get" should {
      "return rate when service returns rate" in new Fixture {
        val currencyPair: Rate.Pair = Rate.Pair(Currency.USD, Currency.EUR)
        val rate: Rate              = Rate(currencyPair, Price(5), Timestamp.now)

        when(ratesServiceMock.get(any[Rate.Pair])).thenReturn(Right(rate))

        val result = program.get(Protocol.GetRatesRequest(currencyPair.from, currencyPair.from))

        result shouldBe Right(rate)
        verify(ratesServiceMock).get(any[Rate.Pair])
      }

      "return error when service returns error" in new Fixture {
        val currencyPair: Rate.Pair = Rate.Pair(Currency.USD, Currency.EUR)

        when(ratesServiceMock.get(any[Rate.Pair])).thenReturn(Left(OneFrameLookupFailed("failed")))

        val result = program.get(Protocol.GetRatesRequest(currencyPair.from, currencyPair.from))

        result shouldBe Left(RateLookupFailed("failed"))
        verify(ratesServiceMock).get(any[Rate.Pair])
      }
    }

  }

  trait Fixture {
    val ratesServiceMock: RatesService[Id] = mock[RatesService[Id]]
    val program: Algebra[Id]               = Program[Id](ratesServiceMock)
  }
}
