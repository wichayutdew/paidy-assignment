package forex.domain.oneframe
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class OneFrameError(error: String)

object OneFrameError {
  implicit val oneFrameErrorDecoder: Decoder[OneFrameError] = deriveDecoder
}
