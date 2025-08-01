package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.config.models.OneFrameConfig
import forex.services.rates.interpreters._
import forex.services.secretManager.errors.{ Error => VaultError }
import org.http4s.client.Client

//$COVERAGE-OFF$
object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def oneFrame[F[_]: Sync](
      client: Client[F],
      config: OneFrameConfig,
      token: F[VaultError Either String]
  ): Algebra[F] = new OneFrameService[F](client, config, token)
}
//$COVERAGE-ON$
