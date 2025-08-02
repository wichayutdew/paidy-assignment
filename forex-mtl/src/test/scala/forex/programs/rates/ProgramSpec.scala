package forex.programs.rates
import cats.effect.IO
import forex.domain.rates.Pair
import forex.helper.MockedObject
import forex.programs.rates.errors.{ Error => ProgramError }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProgramSpec extends AnyWordSpec with Matchers with MockitoSugar with MockedObject with ScalaFutures {
  "Program" when {
    "get" should {
      "return rate successfully" in new Fixture {
        when(oneFrameTokenProgramMocked.getToken).thenReturn(IO.pure(Right("token")))
        when(oneFrameApiProgramMocked.get(any[Pair], any[String])).thenReturn(IO.pure(Right(mockedRate)))

        whenReady(program.get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Right(mockedRate)
            verify(oneFrameTokenProgramMocked).getToken
            verify(oneFrameApiProgramMocked).get(any[Pair], any[String])
        }
      }

      "return error when TokenProgram is failed" in new Fixture {
        when(oneFrameTokenProgramMocked.getToken).thenReturn(
          IO.pure(Left(ProgramError.RateLookupFailed("token not found")))
        )

        whenReady(program.get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Left(ProgramError.RateLookupFailed("token not found"))
            verify(oneFrameTokenProgramMocked).getToken
            verifyZeroInteractions(oneFrameApiProgramMocked)
        }
      }

      "return error when ApiProgram is failed" in new Fixture {
        when(oneFrameTokenProgramMocked.getToken).thenReturn(IO.pure(Right("token")))
        when(oneFrameApiProgramMocked.get(any[Pair], any[String]))
          .thenReturn(IO.pure(Left(ProgramError.RateLookupFailed("api error"))))

        whenReady(program.get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Left(ProgramError.RateLookupFailed("api error"))
            verify(oneFrameTokenProgramMocked).getToken
            verify(oneFrameApiProgramMocked).get(any[Pair], any[String])
        }
      }
    }

    "preFetch" should {
      "call both TokenProgram and APIProgram successfully" in new Fixture {
        when(oneFrameTokenProgramMocked.getToken).thenReturn(IO.pure(Right("token")))

        whenReady(program.preFetch().unsafeToFuture()) { _ =>
          verify(oneFrameTokenProgramMocked).getToken
          verify(oneFrameApiProgramMocked).preFetch(any[String])
        }
      }

      "skip error and do nothing if TokenProgram fails" in new Fixture {
        when(oneFrameTokenProgramMocked.getToken).thenReturn(
          IO.pure(Left(ProgramError.RateLookupFailed("token not found")))
        )

        whenReady(program.preFetch().unsafeToFuture()) { _ =>
          verify(oneFrameTokenProgramMocked).getToken
          verifyZeroInteractions(oneFrameApiProgramMocked)
        }
      }
    }
  }

  trait Fixture {
    val oneFrameTokenProgramMocked: OneFrameTokenProgram[IO] = mock[OneFrameTokenProgram[IO]]
    val oneFrameApiProgramMocked: OneFrameAPIProgram[IO]     = mock[OneFrameAPIProgram[IO]]

    val program: Program[IO] = Program[IO](oneFrameTokenProgramMocked, oneFrameApiProgramMocked)
  }
}
