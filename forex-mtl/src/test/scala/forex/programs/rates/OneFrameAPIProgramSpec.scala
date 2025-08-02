package forex.programs.rates
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import forex.domain.rates.{ Currency, Pair }
import forex.helper.MockedObject
import forex.services.RatesService
import forex.services.cache.interpreters.RedisService
import forex.services.rates.errors.{ Error => ServiceError }
import forex.programs.rates.errors.{ Error => ProgramError }
import io.circe.syntax.EncoderOps
import org.mockito.Mockito.lenient
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{ Logger => Slf4jLogger }

import scala.concurrent.duration.FiniteDuration

class OneFrameAPIProgramSpec extends AnyWordSpec with Matchers with MockitoSugar with MockedObject with ScalaFutures {
  "RatesProgram" when {
    "get without cache" should {
      "return rate successfully" in new Fixture {
        when(ratesServiceMock.get(any[List[Pair]], any[String])).thenReturn(IO.pure(Right(List(mockedRate))))

        whenReady(program().get(Pair(mockedRate.pair.from, mockedRate.pair.to), "token").unsafeToFuture()) { response =>
          response shouldBe Right(mockedRate)
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verifyZeroInteractions(redisServiceMock)
          verifyZeroInteractions(loggerMock)
        }
      }

      "return error when Exchange rate is not found" in new Fixture {
        when(ratesServiceMock.get(any[List[Pair]], any[String])).thenReturn(IO.pure(Right(List.empty)))

        whenReady(program().get(Pair(mockedRate.pair.from, mockedRate.pair.to), "token").unsafeToFuture()) { response =>
          response shouldBe Left(ProgramError.ExchangeRateNotFound(s"${mockedRate.pair.from} to ${mockedRate.pair.to}"))
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verifyZeroInteractions(redisServiceMock)
          verify(loggerMock).error(any[String])
        }
      }

      "return error when RatesService returns error" in new Fixture {
        when(ratesServiceMock.get(any[List[Pair]], any[String]))
          .thenReturn(IO.pure(Left(ServiceError.OneFrameLookupFailed("failed"))))

        whenReady(program().get(Pair(mockedRate.pair.from, mockedRate.pair.to), "token").unsafeToFuture()) { response =>
          response shouldBe Left(ProgramError.RateLookupFailed("failed"))
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verifyZeroInteractions(redisServiceMock)
          verifyZeroInteractions(loggerMock)
        }
      }
    }

    "get with cache" should {
      "return rate from cache successfully" in new Fixture {
        when(redisServiceMock.get(any[String])).thenReturn(IO.pure(Some(mockedRate.asJson.noSpaces)))

        whenReady(
          program(enableCache = true)
            .get(Pair(mockedRate.pair.from, mockedRate.pair.to), "token")
            .unsafeToFuture()
        ) { response =>
          response shouldBe Right(mockedRate)
          verify(redisServiceMock).get(any[String])
          verifyZeroInteractions(ratesServiceMock)
          verifyZeroInteractions(loggerMock)
        }
      }

      "return error when unable to decode cache response" in new Fixture {
        when(redisServiceMock.get(any[String])).thenReturn(IO.pure(Some("invalid json")))

        whenReady(
          program(enableCache = true)
            .get(Pair(mockedRate.pair.from, mockedRate.pair.to), "token")
            .unsafeToFuture()
        ) { response =>
          response.isLeft shouldBe true
          response.left.toOption.get shouldBe a[ProgramError.DecodingFailure]
          verify(redisServiceMock).get(any[String])
          verifyZeroInteractions(ratesServiceMock)
          verify(loggerMock).error(any[String], any[Throwable])
        }
      }

      "return error when Exchange rate is not found" in new Fixture {
        when(redisServiceMock.get(any[String])).thenReturn(IO.pure(None))
        when(ratesServiceMock.get(any[List[Pair]], any[String]))
          .thenReturn(IO.pure(Right(List(mockedRate.copy(pair = Pair(Currency.USD, Currency.CAD))))))

        whenReady(
          program(enableCache = true)
            .get(Pair(mockedRate.pair.from, mockedRate.pair.to), "token")
            .unsafeToFuture()
        ) { response =>
          response shouldBe Left(ProgramError.ExchangeRateNotFound(s"${mockedRate.pair.from} to ${mockedRate.pair.to}"))
          verify(redisServiceMock).get(any[String])
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verify(redisServiceMock).set(any[String], any[String], any[FiniteDuration])
          verify(loggerMock).error(any[String])
        }
      }

      "return rate from api response and put into cache if cache is empty" in new Fixture {
        when(redisServiceMock.get(any[String])).thenReturn(IO.pure(None))
        when(ratesServiceMock.get(any[List[Pair]], any[String])).thenReturn(IO.pure(Right(List(mockedRate))))

        whenReady(
          program(enableCache = true)
            .get(Pair(mockedRate.pair.from, mockedRate.pair.to), "token")
            .unsafeToFuture()
        ) { response =>
          response shouldBe Right(mockedRate)
          verify(redisServiceMock).get(any[String])
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verify(redisServiceMock).set(any[String], any[String], any[FiniteDuration])
          verifyZeroInteractions(loggerMock)
        }
      }

      "return error and not fill cache when RatesService returns error" in new Fixture {
        when(redisServiceMock.get(any[String])).thenReturn(IO.pure(None))
        when(ratesServiceMock.get(any[List[Pair]], any[String]))
          .thenReturn(IO.pure(Left(ServiceError.OneFrameLookupFailed("failed"))))

        whenReady(
          program(enableCache = true)
            .get(Pair(mockedRate.pair.from, mockedRate.pair.to), "token")
            .unsafeToFuture()
        ) { response =>
          response shouldBe Left(ProgramError.RateLookupFailed("failed"))
          verify(redisServiceMock).get(any[String])
          verify(ratesServiceMock).get(any[List[Pair]], any[String])
          verifyNoMoreInteractions(redisServiceMock)
          verifyZeroInteractions(loggerMock)
        }
      }

    }

    "preFetch" should {
      "call fetchRates with all pairs and fill cache" in new Fixture {
        when(ratesServiceMock.get(any[List[Pair]], any[String])).thenReturn(IO.pure(Right(List(mockedRate))))

        whenReady(program(enableCache = true).preFetch("token").unsafeToFuture()) { _ =>
          verify(ratesServiceMock, times(1)).get(Currency.getAllPairs, "token")
          verify(redisServiceMock, times(1)).set(any[String], any[String], any[FiniteDuration])
        }
      }

      "not doing anything if cache is disabled" in new Fixture {
        whenReady(program().preFetch("token").unsafeToFuture()) { _ =>
          verifyZeroInteractions(ratesServiceMock)
          verifyZeroInteractions(redisServiceMock)
        }
      }
    }
  }

  trait Fixture {
    val ratesServiceMock: RatesService[IO] = mock[RatesService[IO]]
    val redisServiceMock: RedisService[IO] = mock[RedisService[IO]]

    val loggerMock: Slf4jLogger = mock[Slf4jLogger]
    lenient().when(loggerMock.isErrorEnabled()).thenReturn(true)

    def program(enableCache: Boolean = false): OneFrameAPIProgram[IO] = new OneFrameAPIProgram[IO](
      ratesServiceMock,
      redisServiceMock,
      mockedApplicationConfig.cache.rates.copy(enabled = enableCache)
    ) {
      override lazy val logger: Logger = Logger(loggerMock)
    }
  }
}
