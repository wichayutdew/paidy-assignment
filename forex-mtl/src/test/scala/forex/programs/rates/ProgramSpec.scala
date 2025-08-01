package forex.programs.rates
import cats.effect.IO
import forex.domain.rates.Pair
import forex.helper.MockedObject
import forex.programs.rates.errors.Error.RateLookupFailed
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.secretManager.errors.Error.SecretLookupFailed
import forex.services.{ RatesService, SecretManagerService }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProgramSpec extends AnyWordSpec with Matchers with MockitoSugar with MockedObject with ScalaFutures {
  "Program" when {
    "get" should {
      "return rate successfully" in new Fixture {
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[Pair], any[String])).thenReturn(IO.pure(Right(mockedRate)))

        whenReady(program.get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Right(mockedRate)
            verify(secretManagerServiceMock).get(any[String], any[String])
            verify(ratesServiceMock).get(any[Pair], any[String])
        }
      }

      "return error when RatesService returns error" in new Fixture {
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[Pair], any[String])).thenReturn(IO.pure(Left(OneFrameLookupFailed("failed"))))

        whenReady(program.get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Left(RateLookupFailed("failed"))
            verify(secretManagerServiceMock).get(any[String], any[String])
            verify(ratesServiceMock).get(any[Pair], any[String])
        }
      }

      "return error when SecretManagerService returns error" in new Fixture {
        when(secretManagerServiceMock.get(any[String], any[String]))
          .thenReturn(IO.pure(Left(SecretLookupFailed("failed"))))

        whenReady(program.get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Left(RateLookupFailed("failed"))
            verify(secretManagerServiceMock).get(any[String], any[String])
            verifyZeroInteractions(ratesServiceMock)
        }
      }
    }

  }

  trait Fixture {
    val ratesServiceMock: RatesService[IO]                 = mock[RatesService[IO]]
    val secretManagerServiceMock: SecretManagerService[IO] = mock[SecretManagerService[IO]]

    val program: Algebra[IO] = Program[IO](ratesServiceMock, secretManagerServiceMock)
  }
}
