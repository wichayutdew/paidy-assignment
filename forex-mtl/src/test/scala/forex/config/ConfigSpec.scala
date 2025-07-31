package forex.config
import cats.effect.IO
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.nio.file.Files
import scala.concurrent.duration.{ FiniteDuration, SECONDS }

class ConfigSpec extends AnyWordSpec with Matchers {
  "Config" when {
    "stream" should {
      "successfully load a valid configuration" in {
        val configFile = createTempConfigFile("""app {
                                                |  server {
                                                |    host = "localhost"
                                                |    port = 8080
                                                |    timeout = 40 seconds
                                                |  }
                                                |  client {
                                                |    one-frame {
                                                |      host = "localhost"
                                                |      port = 8090
                                                |      timeout = 30 seconds
                                                |    }
                                                |  }
                                                |}""".stripMargin)
        System.setProperty("config.file", configFile.getAbsolutePath)

        val result = Config.stream[IO]("app").compile.toList.unsafeRunSync()

        result.size shouldBe 1
        val config: ApplicationConfig = result.head
        config.server.host shouldBe "localhost"
        config.server.port shouldBe 8080
        config.server.timeout shouldBe FiniteDuration(40, SECONDS)

        config.client.oneFrame.host shouldBe "localhost"
        config.client.oneFrame.port shouldBe 8090
        config.client.oneFrame.timeout shouldBe FiniteDuration(30, SECONDS)

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
        // Create a temp config file with invalid content (wrong types)
        val configFile = createTempConfigFile("""invalid {
                                                |  server {
                                                |    host = 1234
                                                |    port = "not-a-number"
                                                |    timeout = 5555
                                                |  }
                                                |  client {
                                                |    one-frame {
                                                |      host = 1234
                                                |      port = "not-a-number"
                                                |      timeout = 312333
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
