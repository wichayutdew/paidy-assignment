package forex.programs.rates
import cats.Id
import forex.domain.Rate
import forex.helper.MockedObject
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.services.RatesService
import forex.services.rates.errors.Error.OneFrameLookupFailed
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProgramSpec extends AnyWordSpec with Matchers with MockitoSugar with MockedObject {
  "Program" when {
    "get" should {
      "return rate when service returns rate" in new Fixture {
        when(ratesServiceMock.get(any[Rate.Pair])).thenReturn(Right(mockedRate))

        val result = program.get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to))

        result shouldBe Right(mockedRate)
        verify(ratesServiceMock).get(any[Rate.Pair])
      }

      "return error when service returns error" in new Fixture {
        when(ratesServiceMock.get(any[Rate.Pair])).thenReturn(Left(OneFrameLookupFailed("failed")))

        val result = program.get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to))

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
