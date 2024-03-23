package text2ql

import cats.implicits.catsSyntaxOptionId
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}
import sttp.model.StatusCode
import sttp.monad.MonadError
import sttp.tapir.generic.auto.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.model.ValuedEndpointOutput
import sttp.tapir.statusCode

import scala.util.control.NoStackTrace

case class EndpointError(
    status: StatusCode,
    statusText: String,
    message: Option[String] = None
) extends NoStackTrace {

  def outputWithCode[F[_]: MonadError](code: StatusCode): F[Option[ValuedEndpointOutput[_]]] = MonadError[F].unit {
    ValuedEndpointOutput(jsonBody[EndpointError], this).prepend(statusCode, code).some
  }
}

object EndpointError extends SchemaDerivation {
  implicit val scEncoder: Encoder[StatusCode] = Encoder[Int].contramap[StatusCode](_.code)
  implicit val scDecoder: Decoder[StatusCode] = Decoder[Int].map(StatusCode(_))
  implicit val codec: Codec[EndpointError]    = deriveCodec

  def tooManyRequests(message: String): EndpointError =
    EndpointError(StatusCode.TooManyRequests, "TooManyRequests", Some(message))

  def internalServerError(message: String): EndpointError =
    EndpointError(StatusCode.InternalServerError, "InternalServerError", Some(message))
}
