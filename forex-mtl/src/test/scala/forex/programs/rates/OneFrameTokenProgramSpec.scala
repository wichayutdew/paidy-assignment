package forex.programs.rates
import cats.effect.IO
import forex.helper.MockedObject
import forex.services.SecretManagerService
import forex.services.cache.interpreters.InMemoryCacheService
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import forex.services.secretManager.errors.{ Error => SecretManagerError }
import forex.programs.rates.errors.{ Error => ProgramError }

import scala.concurrent.duration.FiniteDuration

class OneFrameTokenProgramSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures with MockedObject {
  "OneFrameTokenProgram" when {
    "getToken without cache" should {
      "return token successfully" in new Fixture {
        when(secretManagerService.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))

        whenReady(program().getToken.unsafeToFuture()) { response =>
          response shouldBe Right("token")
          verify(secretManagerService).get(any[String], any[String])
          verifyZeroInteractions(inMemoryCacheService)
        }
      }

      "return error when SecretManagerService returns error" in new Fixture {
        when(secretManagerService.get(any[String], any[String]))
          .thenReturn(IO.pure(Left(SecretManagerError.SecretLookupFailed("failed"))))

        whenReady(program().getToken.unsafeToFuture()) { response =>
          response shouldBe Left(ProgramError.RateLookupFailed("failed"))
          verify(secretManagerService).get(any[String], any[String])
          verifyZeroInteractions(inMemoryCacheService)
        }
      }
    }

    "getToken with cache" should {
      "return token from cache successfully" in new Fixture {
        when(inMemoryCacheService.get(any[String])).thenReturn(IO.pure(Some("token")))

        whenReady(program(enableCache = true).getToken.unsafeToFuture()) { response =>
          response shouldBe Right("token")
          verify(inMemoryCacheService).get(any[String])
          verifyZeroInteractions(secretManagerService)
        }
      }

      "return token successfully when cache empty and fill cache" in new Fixture {
        when(inMemoryCacheService.get(any[String])).thenReturn(IO.pure(None))
        when(secretManagerService.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))

        whenReady(program(enableCache = true).getToken.unsafeToFuture()) { response =>
          response shouldBe Right("token")
          verify(inMemoryCacheService).get(any[String])
          verify(secretManagerService).get(any[String], any[String])
          verify(inMemoryCacheService).set(any[String], any[String], any[FiniteDuration])
        }
      }

      "return error when SecretManagerService returns error" in new Fixture {
        when(inMemoryCacheService.get(any[String])).thenReturn(IO.pure(None))
        when(secretManagerService.get(any[String], any[String]))
          .thenReturn(IO.pure(Left(SecretManagerError.SecretLookupFailed("failed"))))

        whenReady(program(enableCache = true).getToken.unsafeToFuture()) { response =>
          response shouldBe Left(ProgramError.RateLookupFailed("failed"))
          verify(inMemoryCacheService).get(any[String])
          verify(secretManagerService).get(any[String], any[String])
        }
      }
    }
  }

  trait Fixture {
    val secretManagerService: SecretManagerService[IO] = mock[SecretManagerService[IO]]
    val inMemoryCacheService: InMemoryCacheService[IO] = mock[InMemoryCacheService[IO]]

    def program(enableCache: Boolean = false): OneFrameTokenProgram[IO] = new OneFrameTokenProgram[IO](
      secretManagerService,
      inMemoryCacheService,
      mockedApplicationConfig.cache.token.copy(enabled = enableCache)
    )
  }
}
