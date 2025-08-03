package forex.services.secretManager.interpreters
import cats.Applicative
import com.bettercloud.vault.Vault
import forex.domain.core.measurement.logging.{ AppLogger, ErrorLog, WarnLog }
import forex.domain.core.measurement.metrics.{ EventCounter, MetricsTag }
import forex.services.secretManager.Algebra
import forex.services.secretManager.errors._
import io.opentelemetry.api.metrics.Meter

import scala.util.Try

class VaultService[F[_]: Applicative](client: Vault)(implicit meter: Meter) extends Algebra[F] with AppLogger {
  private val successRateCounter: EventCounter = EventCounter("client.success", "VaultService")

  override def get(path: String, key: String): F[Error Either String] =
    Try(client.logical().read(path).getData).toEither match {
      case Right(value) if value.containsKey(key) =>
        successRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "get"))
        Applicative[F].pure(Right(value.get(key)))
      case Right(_) =>
        successRateCounter.record(Map(MetricsTag.STATUS -> true.toString, MetricsTag.OPERATION -> "get"))
        val errorMessage = s"Key '$key' not found at path '$path'"
        logger.log(WarnLog(errorMessage))
        Applicative[F].pure(Left(Error.SecretLookupFailed(errorMessage)))
      case Left(error) =>
        successRateCounter.record(Map(MetricsTag.STATUS -> false.toString, MetricsTag.OPERATION -> "get"))
        logger.log(ErrorLog(s"[Vault] Failed to read secret", Some(error)))
        Applicative[F].pure(Left(Error.SecretLookupFailed(error.getMessage)))
    }
}
