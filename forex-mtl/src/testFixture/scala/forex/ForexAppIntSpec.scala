package forex

import cats.effect._
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.{ Method, Request, Status, Uri }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Try

class ForexAppIntSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(30, Seconds))

  private val baseUrl = "http://localhost:8090"

  private var clientResource: Resource[IO, Client[IO]] = _
  private var client: Client[IO]                       = _

  implicit private val ec: ExecutionContext = ExecutionContext.global
  implicit private val cs: ContextShift[IO] = IO.contextShift(ec)

  override def beforeAll(): Unit = {
    super.beforeAll()

    clientResource = BlazeClientBuilder[IO](ec)
      .withRequestTimeout(30.seconds)
      .withIdleTimeout(10.seconds)
      .resource

    client = clientResource.allocated.unsafeRunSync()._1

    while (Try(client.statusFromString(s"$baseUrl/health").unsafeRunSync().code).getOrElse(-1) != 200)
      Thread.sleep(1000)
  }

  override def afterAll(): Unit = {
    if (clientResource != null) {
      clientResource.allocated.unsafeRunSync()._2.unsafeRunSync()
    }
    super.afterAll()
  }

  "ForexAppIntSpec" when {
    "/rates" should {
      "return valid rates data" in {
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$baseUrl/rates?from=USD&to=EUR"))

        whenReady(client.run(request).use(response => IO.pure(response)).unsafeToFuture()) { response =>
          response.status shouldBe Status.Ok
          whenReady(response.as[String].unsafeToFuture()) { body =>
            body should startWith("""{"from":"USD","to":"EUR","price":""")
          }
        }
      }

      "return cached exchange rate when called with same currency pair" in {
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$baseUrl/rates?from=USD&to=EUR"))

        whenReady(client.run(request).use(response => IO.pure(response)).unsafeToFuture()) { response1 =>
          response1.status shouldBe Status.Ok
          whenReady(client.run(request).use(response => IO.pure(response)).unsafeToFuture()) { response2 =>
            response2.status shouldBe Status.Ok
            whenReady(response1.as[String].unsafeToFuture()) { body1 =>
              whenReady(response2.as[String].unsafeToFuture()) { body2 =>
                body1 shouldBe body2
              }
            }
          }
        }
      }

      "got Invalid currency query parameter validation error" in {
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$baseUrl/rates?from=THB&to=EUR"))

        whenReady(client.run(request).use(response => IO.pure(response)).unsafeToFuture()) { response =>
          response.status shouldBe Status.BadRequest
          whenReady(response.as[String].unsafeToFuture()) { body =>
            body should startWith("""Invalid `from` parameter: Invalid currency: THB: List of supported currencies:""")
          }
        }
      }

      "got Empty currency query parameter validation error" in {
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$baseUrl/rates?from=USD&to="))

        whenReady(client.run(request).use(response => IO.pure(response)).unsafeToFuture()) { response =>
          response.status shouldBe Status.BadRequest
          whenReady(response.as[String].unsafeToFuture()) { body =>
            body shouldBe """Invalid `to` parameter: Empty currency: Currency parameter should not be empty"""
          }
        }
      }
    }

    "/cache/:service/:key" should {
      "return OK when DELETE redis cache" in {
        val request = Request[IO](Method.DELETE, Uri.unsafeFromString(s"$baseUrl/cache/redis/key"))

        whenReady(client.run(request).use(response => IO.pure(response)).unsafeToFuture()) { response =>
          response.status shouldBe Status.Ok
        }
      }

      "return OK when DELETE in-memory cache" in {
        val request = Request[IO](Method.DELETE, Uri.unsafeFromString(s"$baseUrl/cache/memory/key"))

        whenReady(client.run(request).use(response => IO.pure(response)).unsafeToFuture()) { response =>
          response.status shouldBe Status.Ok
        }
      }

      "re-fetch exchange rate if cache got evicted" in {
        val ratesRequest       = Request[IO](Method.GET, Uri.unsafeFromString(s"$baseUrl/rates?from=USD&to=EUR"))
        val cacheDeleteRequest = Request[IO](Method.DELETE, Uri.unsafeFromString(s"$baseUrl/cache/redis/rates_USDEUR"))

        whenReady(client.run(ratesRequest).use(response => IO.pure(response)).unsafeToFuture()) { response1 =>
          response1.status shouldBe Status.Ok
          whenReady(client.run(cacheDeleteRequest).use(response => IO.pure(response)).unsafeToFuture()) {
            cacheDeleteResponse =>
              cacheDeleteResponse.status shouldBe Status.Ok
              whenReady(client.run(ratesRequest).use(response => IO.pure(response)).unsafeToFuture()) { response2 =>
                response2.status shouldBe Status.Ok
                whenReady(response1.as[String].unsafeToFuture()) { body1 =>
                  whenReady(response2.as[String].unsafeToFuture()) { body2 =>
                    body1 should not be body2
                  }
                }
              }
          }
        }
      }

    }
  }
}
