package forex.services.vault.interpreters
import cats.Applicative
import com.bettercloud.vault.Vault
import forex.services.vault.Algebra
import forex.services.vault.errors._

import scala.util.Try

class VaultService[F[_]: Applicative](client: Vault) extends Algebra[F] {
  override def get(path: String, key: String): F[Error Either String] =
    Try(client.logical().read(path).getData).toEither match {
      case Right(value) if value.containsKey(key) => Applicative[F].pure(Right(value.get(key)))
      case Right(_)    => Applicative[F].pure(Left(Error.SecretLookupFailed(s"Key '$key' not found at path '$path'")))
      case Left(error) => Applicative[F].pure(Left(Error.SecretLookupFailed(error.getMessage)))
    }
}
