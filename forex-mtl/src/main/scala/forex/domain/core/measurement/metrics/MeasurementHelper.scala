package forex.domain.core.measurement.metrics
import cats.effect.Sync
import cats.implicits.{ catsSyntaxApplicativeError, toFlatMapOps }

trait MeasurementHelper {
  def measure[F[_]: Sync, A](
      timerMetric: TimerMetric,
      tags: Map[String, String] = Map.empty
  )(block: F[A]): F[A] =
    Sync[F].delay(System.currentTimeMillis()).flatMap { startTime =>
      block.attempt.flatMap { result =>
        Sync[F].delay(System.currentTimeMillis()).flatMap { endTime =>
          val duration: Double = (endTime - startTime).toDouble
          Sync[F].delay(timerMetric.recordDuration(duration, tags)).flatMap { _ =>
            Sync[F].fromEither(result)
          }
        }
      }
    }
}
