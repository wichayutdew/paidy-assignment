package forex.services.secretManager

import forex.services.secretManager.errors._

trait Algebra[F[_]] {
  def get(path: String, key: String): F[Error Either String]
}
