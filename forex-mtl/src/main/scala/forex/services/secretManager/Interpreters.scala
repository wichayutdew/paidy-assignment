package forex.services.secretManager

import cats.Applicative
import com.bettercloud.vault.Vault
import forex.services.secretManager.interpreters.VaultService

//$COVERAGE-OFF$
object Interpreters {
  def vault[F[_]: Applicative](client: Vault) = new VaultService[F](client)
}
//$COVERAGE-ON$
