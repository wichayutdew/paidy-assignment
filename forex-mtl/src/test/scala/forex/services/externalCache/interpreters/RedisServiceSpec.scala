package forex.services.externalCache.interpreters
import cats.effect.IO
import forex.services.externalCache.errors._
import io.lettuce.core.SetArgs
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.{ FiniteDuration, SECONDS }

class RedisServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {
  "RedisService" when {
    "set" should {
      "fill cache successfully" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenReturn("OK")

        whenReady(redisService.set("key", "value", FiniteDuration(30, SECONDS)).unsafeToFuture()) { response =>
          response shouldBe Right("Value set successfully")
        }
      }
      "return CachePutFailed due to unknown error" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenReturn("")

        whenReady(redisService.set("key", "value", FiniteDuration(30, SECONDS)).unsafeToFuture()) { response =>
          response shouldBe Left(Error.CachePutFailed("Failed to set value for key 'key'"))
        }
      }
      "return CachePutFailed due to error while calling redis" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenThrow(new Exception("error"))

        whenReady(redisService.set("key", "value", FiniteDuration(30, SECONDS)).unsafeToFuture()) { response =>
          response shouldBe Left(Error.CachePutFailed("error"))
        }
      }
    }

    "get" should {
      "retrieve cache successfully" in new Fixture {
        when(redisClientMocked.get(any[String])).thenReturn("result")

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe Right("result")
        }
      }
      "return CacheExpired if cache is expired" in new Fixture {
        when(redisClientMocked.get(any[String])).thenReturn(null)

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe Left(Error.CacheExpired("cache 'key' expired"))
        }
      }
      "return CacheGetFailed due to error while calling redis" in new Fixture {
        when(redisClientMocked.get(any[String])).thenThrow(new Exception("error"))

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe Left(Error.CacheGetFailed("error"))
        }
      }
    }
  }

  trait Fixture {
    val redisClientMocked: RedisCommands[String, String] = mock[RedisCommands[String, String]]
    val redisService                                     = new RedisService[IO](redisClientMocked)
  }
}
