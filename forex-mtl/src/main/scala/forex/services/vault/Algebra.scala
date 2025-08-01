package forex.services.vault

import forex.services.vault.errors._

trait Algebra[F[_]] {
  def get(path: String, key: String): F[Error Either String]
}
