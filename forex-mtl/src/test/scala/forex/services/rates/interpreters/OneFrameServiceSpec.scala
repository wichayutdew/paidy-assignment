package forex.services.rates.interpreters
import cats.effect.{ IO, Resource }
import forex.domain.rates.Rate
import forex.helper.MockedObject
import forex.services.rates.errors.Error.{ DecodingFailure, ExchangeRateNotFound, InvalidToken, OneFrameLookupFailed }
import forex.services.vault.errors.Error.SecretLookupFailed
import forex.services.vault.errors._
import org.http4s.client.Client
import org.http4s.{ Request, Response, Status }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class OneFrameServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with MockedObject with ScalaFutures {

  "OneFrameService" when {
    "get" should {
      "return a rate for a valid pair" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.Ok).withEntity(
          """[{"from":"USD","to":"EUR","bid":1.2,"ask":1.3,"price":1.2,"time_stamp":"2099-12-31T23:59:59Z"}]"""
        )
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service().get(mockedRate.pair).unsafeToFuture()) { response =>
          response shouldBe Right(Rate.fromOneFrameDTO(mockedRateDTO))
        }
      }

      "return ExchangeRateNotFound if no response from OneFrame" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.Ok).withEntity("[]")
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service().get(mockedRate.pair).unsafeToFuture()) { response =>
          response shouldBe Left(ExchangeRateNotFound(s"${mockedRate.pair.from} to ${mockedRate.pair.to}"))
        }
      }

      "return OneFrameLookupFailed if OneFrame returns an error" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.BadGateway).withEntity("Error")
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service().get(mockedRate.pair).unsafeToFuture()) { response =>
          response shouldBe Left(OneFrameLookupFailed("Error"))
        }
      }

      "return DecodingFailure if response cannot be decoded" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.Ok).withEntity("""[{"from":"USD}]""")
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service().get(mockedRate.pair).unsafeToFuture()) { response =>
          response.isLeft shouldBe true
          response.left.toOption.get shouldBe a[DecodingFailure]
        }
      }

      "return InvalidToken if token is invalid" in new Fixture {
        private val invalidToken = IO.pure(Left(SecretLookupFailed("secret not found")))
        whenReady(service(token = invalidToken).get(mockedRate.pair).unsafeToFuture()) { response =>
          response shouldBe Left(InvalidToken("secret not found"))
        }
      }
    }
  }

  trait Fixture {
    val mockedClient: Client[IO]             = mock[Client[IO]]
    val mockedToken: IO[Error Either String] = IO.pure(Right("mocked-token"))

    def service(token: IO[Error Either String] = mockedToken): OneFrameService[IO] = new OneFrameService[IO](
      mockedClient,
      mockedApplicationConfig.client.oneFrame,
      token
    )
  }
}
