package forex.config
import cats.effect.IO
import forex.config.models.ApplicationConfig
import forex.helper.MockedObject
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.nio.file.Files
import scala.concurrent.duration.{ FiniteDuration, SECONDS }

class ConfigSpec extends AnyWordSpec with Matchers with MockedObject {
  "Config" when {
    "stream" should {
      "successfully load a valid configuration" in {
        val configFile = createTempConfigFile(mockedConfig)
        System.setProperty("config.file", configFile.getAbsolutePath)

        val result = Config.stream[IO]("app").compile.toList.unsafeRunSync()

        result.size shouldBe 1
        val config: ApplicationConfig = result.head
        config.server.host shouldBe "localhost"
        config.server.port shouldBe 8080
        config.server.requestTimeout shouldBe FiniteDuration(40, SECONDS)

        config.client.oneFrame.host shouldBe "localhost"
        config.client.oneFrame.port shouldBe 8090
        config.client.oneFrame.requestTimeout shouldBe FiniteDuration(30, SECONDS)
        config.client.oneFrame.connectionTimeout shouldBe FiniteDuration(30, SECONDS)
        config.client.oneFrame.token shouldBe "test-token"

        configFile.delete()
      }

      "throw an exception for missing configuration" in {
        val configFile = createTempConfigFile("""app {}""".stripMargin)
        System.setProperty("config.file", configFile.getAbsolutePath)

        val exception = intercept[pureconfig.error.ConfigReaderException[_]] {
          Config.stream[IO]("non-existent-path").compile.toList.unsafeRunSync()
        }

        exception.getMessage should include("non-existent-path")

        configFile.delete()
      }

      "throw an exception for invalid configuration" in {
        val configFile = createTempConfigFile("""invalid {
                                                |  server {
                                                |    host = 1234
                                                |    port = "not-a-number"
                                                |    request-timeout = 5555
                                                |  }
                                                |  client {
                                                |    one-frame {
                                                |      host = 1234
                                                |      port = "not-a-number"
                                                |      request-timeout = 312333
                                                |      connection-timeout = asd
                                                |      token = "test-token"
                                                |    }
                                                |  }
                                                |}""".stripMargin)

        System.setProperty("config.file", configFile.getAbsolutePath)

        val exception = intercept[pureconfig.error.ConfigReaderException[_]] {
          Config.stream[IO]("invalid").compile.toList.unsafeRunSync()
        }

        exception.getMessage should include("invalid")

        configFile.delete()
      }
    }
  }

  private def createTempConfigFile(content: String): File = {
    val tempFile = File.createTempFile("application", ".conf")
    Files.write(tempFile.toPath, content.getBytes)
    tempFile
  }
}
