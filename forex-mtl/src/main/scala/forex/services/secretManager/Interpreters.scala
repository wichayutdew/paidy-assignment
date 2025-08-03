package forex.services.secretManager

import cats.effect.Sync
import com.bettercloud.vault.Vault
import forex.services.secretManager.interpreters.VaultService
import io.opentelemetry.api.metrics.Meter

//$COVERAGE-OFF$
object Interpreters {
  def vault[F[_]: Sync](client: Vault)(implicit meter: Meter) = new VaultService[F](client)
}
//$COVERAGE-ON$
