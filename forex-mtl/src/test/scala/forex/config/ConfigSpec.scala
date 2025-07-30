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
                                                |  http {
                                                |    host = "localhost"
                                                |    port = 8080
                                                |    timeout = 40 seconds
                                                |  }
                                                |}""".stripMargin)
        System.setProperty("config.file", configFile.getAbsolutePath)

        val result = Config.stream[IO]("app").compile.toList.unsafeRunSync()

        result.size shouldBe 1
        val config: ApplicationConfig = result.head
        config.http.host shouldBe "localhost"
        config.http.port shouldBe 8080
        config.http.timeout shouldBe FiniteDuration(40, SECONDS)

        configFile.delete()
      }

      "throw an exception for missing configuration" in {
        val configFile = createTempConfigFile("""app {
                                                |  http {
                                                |    host = "localhost"
                                                |    port = 8080
                                                |    timeout = 40 seconds
                                                |  }
                                                |}""".stripMargin)
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
                                                |  http {
                                                |    host = 1234
                                                |    port = "not-a-number"
                                                |    timeout = 5555
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
