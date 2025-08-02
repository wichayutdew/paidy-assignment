package forex.services.cache.interpreters
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import io.lettuce.core.SetArgs
import io.lettuce.core.api.sync.RedisCommands
import org.mockito.Mockito.{ lenient, verifyNoInteractions }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{ Logger => Slf4jLogger }

import scala.concurrent.duration.{ FiniteDuration, SECONDS }

class RedisServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {
  "RedisService" when {
    "set" should {
      "fill cache successfully" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenReturn("OK")

        redisService.set("key", "value", FiniteDuration(30, SECONDS))

        verify(redisClientMocked).set(any[String], any[String], any[SetArgs])
        verifyNoInteractions(loggerMock)
      }

      "log warn when set unsuccessfully" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenReturn("invalid")

        redisService.set("key", "value", FiniteDuration(30, SECONDS))

        verify(redisClientMocked).set(any[String], any[String], any[SetArgs])
        verify(loggerMock).warn(any[String])
      }

      "log error when set error" in new Fixture {
        when(redisClientMocked.set(any[String], any[String], any[SetArgs])).thenThrow(new Exception("error"))

        redisService.set("key", "value", FiniteDuration(30, SECONDS))

        verify(redisClientMocked).set(any[String], any[String], any[SetArgs])
        verify(loggerMock).error(any[String], any[Throwable])
      }
    }

    "get" should {
      "retrieve cache successfully" in new Fixture {
        when(redisClientMocked.get(any[String])).thenReturn("result")

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe Some("result")
          verifyNoInteractions(loggerMock)
        }
      }

      "return None due if result is null" in new Fixture {
        when(redisClientMocked.get(any[String])).thenReturn(null)

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe None
          verify(loggerMock).warn(any[String])
        }
      }

      "return None due to error while calling redis" in new Fixture {
        when(redisClientMocked.get(any[String])).thenThrow(new Exception("error"))

        whenReady(redisService.get("key").unsafeToFuture()) { response =>
          response shouldBe None
          verify(loggerMock).error(any[String], any[Throwable])
        }
      }
    }

    "delete" should {
      "delete cache successfully" in new Fixture {
        when(redisClientMocked.del(any[String])).thenReturn(1L)

        redisService.delete("key")

        verify(redisClientMocked).del(any[String])
      }

      "log warn when delete un successfully" in new Fixture {
        when(redisClientMocked.del(any[String])).thenReturn(0L)

        redisService.delete("key")

        verify(redisClientMocked).del(any[String])
        verify(loggerMock).warn(any[String])
      }

      "log error when delete error" in new Fixture {
        when(redisClientMocked.del(any[String])).thenThrow(new Exception("error"))

        redisService.delete("key")

        verify(redisClientMocked).del(any[String])
        verify(loggerMock).error(any[String], any[Throwable])
      }
    }
  }

  trait Fixture {
    val loggerMock: Slf4jLogger = mock[Slf4jLogger]

    lenient().when(loggerMock.isWarnEnabled()).thenReturn(true)
    lenient().when(loggerMock.isErrorEnabled()).thenReturn(true)

    val redisClientMocked: RedisCommands[String, String] = mock[RedisCommands[String, String]]
    val redisService: RedisService[IO]                   = new RedisService[IO](redisClientMocked) {
      override lazy val logger: Logger = Logger(loggerMock)
    }
  }
}
