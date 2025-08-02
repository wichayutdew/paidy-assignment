package forex

import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = new Application[IO]
    .stream(executionContext)
    .compile
    .drain
    .as(ExitCode.Success)
}

class Application[F[_]: ConcurrentEffect: Timer] {
  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app") // REFERENCE FROM TOP LEVEL CONFIG NAME IN `application.conf`
      module <- Stream.resource(Module(config, ec))
      _ <- BlazeServerBuilder[F](ec)
             .bindHttp(config.server.port, config.server.host)
             .withHttpApp(module.httpApp)
             .serve
    } yield ()
}
