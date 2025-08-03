package forex

import cats.effect._
import forex.domain.core.measurement.logging.{ AppLogger, InfoLog }
import forex.domain.rates.Currency
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.http4s.{ Method, Request, Uri }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Random, Try }

class LoadTestSpec extends AnyWordSpec with BeforeAndAfterAll with AppLogger {
  private val baseUrl = "http://localhost:8090"

  private var clientResource: Resource[IO, Client[IO]] = _
  private var client: Client[IO]                       = _

  implicit private val ec: ExecutionContext = ExecutionContext.global
  implicit private val cs: ContextShift[IO] = IO.contextShift(ec)

  override def beforeAll(): Unit = {
    super.beforeAll()

    clientResource = BlazeClientBuilder[IO](ec)
      .withRequestTimeout(30.seconds)
      .withIdleTimeout(60.seconds)
      .resource

    client = clientResource.allocated.unsafeRunSync()._1

    val maxRetries = 30
    var retries    = 0
    while (
      Try(client.statusFromString(s"$baseUrl/health").unsafeRunSync().code).getOrElse(-1) != 200 && retries < maxRetries
    ) {
      Thread.sleep(1000)
      retries += 1
    }
    if (retries == maxRetries) throw new RuntimeException("App did not start in time")
  }

  override def afterAll(): Unit = {
    if (clientResource != null) {
      clientResource.allocated.unsafeRunSync()._2.unsafeRunSync()
    }
    super.afterAll()
  }

  // adding THB, MYR to test wrong currency scenario
  val allCurrencies: List[String] = Currency.supportedCurrencies ++ List("THB", "MYR")
  val random: Random              = new Random()

  private def generateRandomPair: (String, String) = {
    val num1 = random.between(0, allCurrencies.size)
    var num2 = random.between(0, allCurrencies.size)
    while (num2 == num1) num2 = random.between(0, allCurrencies.size)
    (allCurrencies(num1), allCurrencies(num2))
  }
  private def createRandomRequest: Request[IO] = {
    val (from, to) = generateRandomPair
    Request[IO](Method.GET, Uri.unsafeFromString(s"$baseUrl/rates?from=$from&to=$to"))
  }

  private def randomlyDeleteCache(): Request[IO] = {
    val (from, to) = generateRandomPair
    Request[IO](Method.DELETE, Uri.unsafeFromString(s"$baseUrl/cache/redis/rates_$from$to"))
  }

  "LoadTestSpec" should {
    "Call to /rates with random currency pair and randomly evict cache 10,000 times" in
      (0 to 10000).foreach { round =>
        val ratesRequest: Request[IO] = createRandomRequest
        logger.log(InfoLog(s"Round $round: Calling /rates with ${ratesRequest.queryString}"))
        client.run(ratesRequest).use(response => IO.pure(response)).unsafeRunSync()
        if (random.nextBoolean()) {
          val cacheDeleteRequest = randomlyDeleteCache()
          logger.log(InfoLog(s"Round $round: Cache evicted"))
          client.run(cacheDeleteRequest).use(response => IO.pure(response)).unsafeRunSync()
        }
      }
  }

}
