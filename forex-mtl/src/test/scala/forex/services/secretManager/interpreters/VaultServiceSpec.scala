package forex.services.secretManager.interpreters
import cats.effect.IO
import com.bettercloud.vault.Vault
import com.bettercloud.vault.api.Logical
import com.bettercloud.vault.response.LogicalResponse
import com.typesafe.scalalogging.Logger
import forex.services.secretManager.errors._
import org.mockito.Mockito.lenient
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{ Logger => Slf4jLogger }

import scala.jdk.CollectionConverters._

class VaultServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {
  "VaultService" when {
    "get" should {
      "return a token when the path and key are valid" in new Fixture {
        when(logicalMocked.read(any[String])).thenReturn(logicalResponseMocked)
        when(logicalResponseMocked.getData).thenReturn(Map("key" -> "valid-token").asJava)

        whenReady(vaultService.get("path", "key").unsafeToFuture()) { response =>
          response shouldBe Right("valid-token")
          verifyZeroInteractions(loggerMock)
        }
      }
      "return an error when the key is invalid" in new Fixture {
        when(logicalMocked.read(any[String])).thenReturn(logicalResponseMocked)
        when(logicalResponseMocked.getData).thenReturn(Map("token" -> "valid-token").asJava)

        whenReady(vaultService.get("path", "key").unsafeToFuture()) { response =>
          response shouldBe Left(Error.SecretLookupFailed("Key 'key' not found at path 'path'"))
          verify(loggerMock).warn(any[String])
        }
      }

      "return an error when the path is invalid" in new Fixture {
        when(logicalMocked.read(any[String])).thenThrow(new RuntimeException("Invalid path"))

        whenReady(vaultService.get("path", "key").unsafeToFuture()) { response =>
          response shouldBe Left(Error.SecretLookupFailed("Invalid path"))
          verify(loggerMock).error(any[String], any[Throwable])
        }
      }
    }
  }

  trait Fixture {
    val vaultClientMocked: Vault               = mock[Vault]
    val logicalMocked: Logical                 = mock[Logical]
    val logicalResponseMocked: LogicalResponse = mock[LogicalResponse]
    when(vaultClientMocked.logical()).thenReturn(logicalMocked)

    val loggerMock: Slf4jLogger = mock[Slf4jLogger]
    lenient().when(loggerMock.isWarnEnabled()).thenReturn(true)
    lenient().when(loggerMock.isErrorEnabled()).thenReturn(true)

    val vaultService: VaultService[IO] = new VaultService[IO](vaultClientMocked) {
      override lazy val logger: Logger = Logger(loggerMock)
    }
  }
}
