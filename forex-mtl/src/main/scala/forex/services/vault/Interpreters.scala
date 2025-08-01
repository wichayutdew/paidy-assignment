package forex.services.vault

import cats.Applicative
import com.bettercloud.vault.Vault
import forex.services.vault.interpreters.VaultService

//$COVERAGE-OFF$
object Interpreters {
  def vault[F[_]: Applicative](client: Vault) = new VaultService[F](client)
}
//$COVERAGE-ON$
