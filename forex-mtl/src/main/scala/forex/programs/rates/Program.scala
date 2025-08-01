package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import forex.domain.rates.{ Pair, Rate }
import forex.domain.vault.Constant.{ Key, Path }
import forex.programs.rates.errors._
import forex.services.{ RatesService, SecretManagerService }

class Program[F[_]: Monad](
    ratesService: RatesService[F],
    secretManagerService: SecretManagerService[F]
) extends Algebra[F] {

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = (for {
    token <- EitherT(secretManagerService.get(Path.ONE_FRAME, Key.TOKEN)).leftMap(toProgramError)
    rates <- EitherT(ratesService.get(Pair(request.from, request.to), token)).leftMap(toProgramError)
  } yield rates).value

}

object Program {

  def apply[F[_]: Monad](
      ratesService: RatesService[F],
      secretManagerService: SecretManagerService[F]
  ): Algebra[F] = new Program[F](ratesService, secretManagerService)

}
