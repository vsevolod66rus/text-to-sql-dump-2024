package text2ql.domainschema

import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, Encoder}
import sttp.tapir.Schema
import text2ql.api._

final case class CheckDomainSchemaResponse(
    entities: List[String],
    queries: BuildQueryDTO,
    result: Either[String, GridWithDataRenderTypeResponseModel]
)

object CheckDomainSchemaResponse {
  implicit lazy val buildQueryDTOCodec: Codec[BuildQueryDTO]   = deriveCodec
  implicit lazy val buildQueryDTOSchema: Schema[BuildQueryDTO] = Schema.derived
  implicit lazy val codec: Codec[CheckDomainSchemaResponse]    = deriveCodec
  implicit lazy val schema: Schema[CheckDomainSchemaResponse]  = Schema.derived

  implicit def eitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A, B]] = {
    val left: Decoder[Either[A, B]]  = a.map(Left.apply)
    val right: Decoder[Either[A, B]] = b.map(Right.apply)
    left.or(right)
  }

  implicit def eitherEncoder[A, B](implicit a: Encoder[A], b: Encoder[B]): Encoder[Either[A, B]] = { o: Either[A, B] =>
    o.fold(_.asJson, _.asJson)
  }
}
