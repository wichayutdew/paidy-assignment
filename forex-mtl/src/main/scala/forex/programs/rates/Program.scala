package forex.programs.rates

import cats.Monad
import cats.data.EitherT
import cats.implicits.toFunctorOps
import forex.domain.rates.{ Pair, Rate }
import forex.programs.rates.errors._

class Program[F[_]: Monad](
    oneFrameTokenProgram: OneFrameTokenProgram[F],
    oneFrameApiProgram: OneFrameAPIProgram[F]
) extends Algebra[F] {
  override def preFetch(): F[Unit] = (for {
    token <- EitherT(oneFrameTokenProgram.getToken)
  } yield oneFrameApiProgram.preFetch(token)).value.void

  override def get(request: Protocol.GetRatesRequest): F[Error Either Rate] = {
    val exchangeRatePair = Pair(request.from, request.to)
    (for {
      token <- EitherT(oneFrameTokenProgram.getToken)
      rates <- EitherT(oneFrameApiProgram.get(exchangeRatePair, token))
    } yield rates).value
  }
}

object Program {

  def apply[F[_]: Monad](
      oneFrameTokenProgram: OneFrameTokenProgram[F],
      oneFrameApiProgram: OneFrameAPIProgram[F]
  ): Program[F] = new Program[F](
    oneFrameTokenProgram,
    oneFrameApiProgram
  )

}
