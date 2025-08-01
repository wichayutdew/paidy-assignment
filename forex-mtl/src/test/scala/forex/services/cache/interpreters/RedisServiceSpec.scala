package forex.services.cache.interpreters
import cats.effect.IO
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

        redisService.set("key", "value", FiniteDuration(30, SECONDS))

        verify(redisClientMocked).set(any[String], any[String], any[SetArgs])
      }
    }

    "get" should {
      "retrieve cache successfully" in new Fixture {
        when(redisClientMocked.get(any[String])).thenReturn("result")

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe Some("result")
        }
      }
      "return None due to error while calling redis" in new Fixture {
        when(redisClientMocked.get(any[String])).thenThrow(new Exception("error"))

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe None
        }
      }
    }
  }

  trait Fixture {
    val redisClientMocked: RedisCommands[String, String] = mock[RedisCommands[String, String]]
    val redisService                                     = new RedisService[IO](redisClientMocked)
  }
}
