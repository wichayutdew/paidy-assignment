package forex.services.cache.interpreters
import cats.effect.IO
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.{ FiniteDuration, SECONDS }

class InMemoryCacheServiceSpec extends AnyWordSpec with Matchers with ScalaFutures {
  "InMemoryCacheService" when {
    "get" should {
      "return value if key exists" in new Fixture {
        service.set("key", "value", FiniteDuration(10, SECONDS))
        whenReady(service.get("key").unsafeToFuture()) { result =>
          result shouldBe Some("value")
        }
      }

      "return None if cache is expired" in new Fixture {
        service.set("key", "value", FiniteDuration(0, SECONDS))
        whenReady(service.get("key").unsafeToFuture()) { result =>
          result shouldBe None
        }
      }

      "return None if key is not exists" in new Fixture {
        whenReady(service.get("key").unsafeToFuture()) { result =>
          result shouldBe None
        }
      }
    }
  }

  trait Fixture {
    val service: InMemoryCacheService[IO] = new InMemoryCacheService[IO]
  }
}
