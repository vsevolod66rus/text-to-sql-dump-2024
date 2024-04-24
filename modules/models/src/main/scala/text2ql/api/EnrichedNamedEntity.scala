package text2ql.api

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema

case class EnrichedNamedEntity(
    tag: String,
    token: String,
    value: String,
    role: Option[String] = None,
    group: Option[Int] = None,
    isTarget: Boolean = false
) {

  def filterByTagAndValueOpt: (String, Option[String]) => Boolean = (name, value) =>
    tag == name && value.contains(token)

  def filterByGroupOpt: Option[Int] => Boolean = filter => group.exists(filter.contains)
}

object EnrichedNamedEntity {
  implicit val codec: Codec[EnrichedNamedEntity]   = deriveCodec
  implicit val schema: Schema[EnrichedNamedEntity] = Schema.derived
}
