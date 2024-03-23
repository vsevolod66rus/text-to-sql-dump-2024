package text2ql.api

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

case class CountQueryDTO(
    countRecords: Long,
    countTarget: Option[Long],
    query: String
)

case class CountQueryResultUnique(
    countRecords: Long,
    countTarget: Option[Long]
)

object CountQueryDTO {
  implicit val codec: Codec[CountQueryDTO]   = deriveCodec
  implicit val schema: Schema[CountQueryDTO] = Schema.derived
}
