package forex.programs.cache
import cats.effect.IO
import forex.services.cache.interpreters.{ InMemoryCacheService, RedisService }
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ProgramSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {
  "Program" when {
    "delete" should {
      "call RedisService successfully" in new Fixture {
        whenReady(program.delete("testKey", "redis").unsafeToFuture()) { _ =>
          verify(redisServiceMocked).delete("testKey")
          verifyZeroInteractions(inMemoryCacheServiceMocked)
        }
      }
      "call InMemoryCacheService successfully" in new Fixture {
        whenReady(program.delete("testKey", "memory").unsafeToFuture()) { _ =>
          verifyZeroInteractions(redisServiceMocked)
          verify(inMemoryCacheServiceMocked).delete("testKey")
        }
      }

      "not call to any service" in new Fixture {
        whenReady(program.delete("testKey", "random").unsafeToFuture()) { _ =>
          verifyZeroInteractions(redisServiceMocked)
          verifyZeroInteractions(inMemoryCacheServiceMocked)
        }
      }
    }
  }
  trait Fixture {
    val redisServiceMocked: RedisService[IO]                 = mock[RedisService[IO]]
    val inMemoryCacheServiceMocked: InMemoryCacheService[IO] = mock[InMemoryCacheService[IO]]
    val program: Program[IO]                                 = Program(redisServiceMocked, inMemoryCacheServiceMocked)
  }
}
