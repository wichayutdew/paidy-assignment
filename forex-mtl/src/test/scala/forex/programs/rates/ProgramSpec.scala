package forex.programs.rates
import cats.effect.IO
import forex.domain.rates.{ Currency, Pair }
import forex.helper.MockedObject
import forex.programs.rates.errors.Error.{ ExchangeRateNotFound, RateLookupFailed }
import forex.services.rates.errors.Error.OneFrameLookupFailed
import forex.services.secretManager.errors.Error.SecretLookupFailed
import forex.services.{ CacheService, RatesService, SecretManagerService }
import io.circe.syntax.EncoderOps
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.FiniteDuration

class ProgramSpec extends AnyWordSpec with Matchers with MockitoSugar with MockedObject with ScalaFutures {
  "Program" when {
    "get without cache" should {
      "return rate successfully" in new Fixture {
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[List[Pair]], any[String])).thenReturn(IO.pure(Right(List(mockedRate))))

        whenReady(program().get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Right(mockedRate)
            verify(secretManagerServiceMock).get(any[String], any[String])
            verify(ratesServiceMock).get(any[List[Pair]], any[String])
            verifyZeroInteractions(cacheServiceMock)
        }
      }

      "return error when Exchange rate is not found" in new Fixture {
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[List[Pair]], any[String])).thenReturn(IO.pure(Right(List.empty)))

        whenReady(program().get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Left(ExchangeRateNotFound(s"${mockedRate.pair.from} to ${mockedRate.pair.to}"))
            verify(secretManagerServiceMock).get(any[String], any[String])
            verify(ratesServiceMock).get(any[List[Pair]], any[String])
            verifyZeroInteractions(cacheServiceMock)
        }
      }

      "return error when RatesService returns error" in new Fixture {
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[List[Pair]], any[String]))
          .thenReturn(IO.pure(Left(OneFrameLookupFailed("failed"))))

        whenReady(program().get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Left(RateLookupFailed("failed"))
            verify(secretManagerServiceMock).get(any[String], any[String])
            verify(ratesServiceMock).get(any[List[Pair]], any[String])
            verifyZeroInteractions(cacheServiceMock)
        }
      }

      "return error when SecretManagerService returns error" in new Fixture {
        when(secretManagerServiceMock.get(any[String], any[String]))
          .thenReturn(IO.pure(Left(SecretLookupFailed("failed"))))

        whenReady(program().get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to)).unsafeToFuture()) {
          response =>
            response shouldBe Left(RateLookupFailed("failed"))
            verify(secretManagerServiceMock).get(any[String], any[String])
            verifyZeroInteractions(ratesServiceMock)
            verifyZeroInteractions(cacheServiceMock)
        }
      }
    }

    "get with cache" should {
      "return rate from cache successfully" in new Fixture {
        when(cacheServiceMock.get(any[String])).thenReturn(IO.pure(Some(mockedRate.asJson.noSpaces)))

        whenReady(
          program(enableCache = true)
            .get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to))
            .unsafeToFuture()
        ) { response =>
          response shouldBe Right(mockedRate)
          verify(cacheServiceMock).get(any[String])
          verifyZeroInteractions(secretManagerServiceMock)
          verifyZeroInteractions(ratesServiceMock)
        }
      }

      "return error when Exchange rate is not found" in new Fixture {
        when(cacheServiceMock.get(any[String])).thenReturn(IO.pure(None))
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[List[Pair]], any[String]))
          .thenReturn(IO.pure(Right(List(mockedRate.copy(pair = Pair(Currency.USD, Currency.CAD))))))

        whenReady(
          program(enableCache = true)
            .get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to))
            .unsafeToFuture()
        ) { response =>
          response shouldBe Left(ExchangeRateNotFound(s"${mockedRate.pair.from} to ${mockedRate.pair.to}"))
          verify(cacheServiceMock).get(any[String])
          verify(secretManagerServiceMock).get(any[String], any[String])
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verify(cacheServiceMock).set(any[String], any[String], any[FiniteDuration])
        }
      }

      "return rate from api response and put into cache if cache is empty" in new Fixture {
        when(cacheServiceMock.get(any[String])).thenReturn(IO.pure(None))
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[List[Pair]], any[String])).thenReturn(IO.pure(Right(List(mockedRate))))

        whenReady(
          program(enableCache = true)
            .get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to))
            .unsafeToFuture()
        ) { response =>
          response shouldBe Right(mockedRate)
          verify(cacheServiceMock).get(any[String])
          verify(secretManagerServiceMock).get(any[String], any[String])
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verify(cacheServiceMock).set(any[String], any[String], any[FiniteDuration])
        }
      }

      "return error and not fill cache when RatesService returns error" in new Fixture {
        when(cacheServiceMock.get(any[String])).thenReturn(IO.pure(None))
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[List[Pair]], any[String]))
          .thenReturn(IO.pure(Left(OneFrameLookupFailed("failed"))))

        whenReady(
          program(enableCache = true)
            .get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to))
            .unsafeToFuture()
        ) { response =>
          response shouldBe Left(RateLookupFailed("failed"))
          verify(cacheServiceMock).get(any[String])
          verify(secretManagerServiceMock).get(any[String], any[String])
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verifyNoMoreInteractions(cacheServiceMock)
        }
      }

      "return error and not fill cache when SecretManagerService returns error" in new Fixture {
        when(cacheServiceMock.get(any[String])).thenReturn(IO.pure(None))
        when(secretManagerServiceMock.get(any[String], any[String]))
          .thenReturn(IO.pure(Left(SecretLookupFailed("failed"))))

        whenReady(
          program(enableCache = true)
            .get(Protocol.GetRatesRequest(mockedRate.pair.from, mockedRate.pair.to))
            .unsafeToFuture()
        ) { response =>
          response shouldBe Left(RateLookupFailed("failed"))
          verify(cacheServiceMock).get(any[String])
          verify(secretManagerServiceMock).get(any[String], any[String])
          verifyZeroInteractions(ratesServiceMock)
          verifyNoMoreInteractions(cacheServiceMock)
        }
      }
    }

    "preFetch" should {
      "call fetchRates with all pairs and fill cache" in new Fixture {
        when(secretManagerServiceMock.get(any[String], any[String])).thenReturn(IO.pure(Right("token")))
        when(ratesServiceMock.get(any[List[Pair]], any[String])).thenReturn(IO.pure(Right(List(mockedRate))))

        whenReady(program(enableCache = true).preFetch().unsafeToFuture()) { _ =>
          verify(secretManagerServiceMock, times(1)).get(any[String], any[String])
          verify(ratesServiceMock, times(1)).get(Currency.getAllPairs, "token")
          verify(cacheServiceMock, times(1)).set(any[String], any[String], any[FiniteDuration])
        }
      }
    }
  }

  trait Fixture {
    val ratesServiceMock: RatesService[IO]                 = mock[RatesService[IO]]
    val secretManagerServiceMock: SecretManagerService[IO] = mock[SecretManagerService[IO]]
    val cacheServiceMock: CacheService[IO]                 = mock[CacheService[IO]]

    def program(enableCache: Boolean = false): Program[IO] = new Program[IO](
      ratesServiceMock,
      secretManagerServiceMock,
      cacheServiceMock,
      mockedApplicationConfig.cache.rates.copy(enabled = enableCache)
    )
  }
}
