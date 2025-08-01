package forex.services.rates.interpreters
import cats.effect.{ IO, Resource }
import forex.domain.rates.Rate
import forex.helper.MockedObject
import forex.services.rates.errors.Error.{ DecodingFailure, OneFrameLookupFailed }
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

        whenReady(service.get(List(mockedRate.pair), mockedToken).unsafeToFuture()) { response =>
          response shouldBe Right(List(Rate.fromOneFrameDTO(mockedRateDTO)))
        }
      }

      "return OneFrameLookupFailed if OneFrame returns an error" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.BadGateway).withEntity("Error")
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service.get(List(mockedRate.pair), mockedToken).unsafeToFuture()) { response =>
          response shouldBe Left(OneFrameLookupFailed("Error"))
        }
      }

      "return DecodingFailure if response cannot be decoded" in new Fixture {
        val mockResponse: Response[IO] = Response[IO](status = Status.Ok).withEntity("""[{"from":"USD}]""")
        when(mockedClient.run(any[Request[IO]])).thenReturn(Resource.pure[IO, Response[IO]](mockResponse))

        whenReady(service.get(List(mockedRate.pair), mockedToken).unsafeToFuture()) { response =>
          response.isLeft shouldBe true
          response.left.toOption.get shouldBe a[DecodingFailure]
        }
      }
    }
  }

  trait Fixture {
    val mockedClient: Client[IO] = mock[Client[IO]]

    val service: OneFrameService[IO] = new OneFrameService[IO](
      mockedClient,
      mockedApplicationConfig.client.oneFrame
    )
  }
}
