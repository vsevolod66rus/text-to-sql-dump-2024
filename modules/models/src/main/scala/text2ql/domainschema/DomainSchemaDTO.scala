package text2ql.domainschema

import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import sttp.tapir.Schema

final case class DomainSchemaDTO(
    vertices: List[DomainSchemaVertex],
    edges: List[DomainSchemaEdge],
    attributes: List[DomainSchemaAttribute]
)

final case class DomainSchemaVertex(
    vertexName: String,
    title: String,
    alternatives: Option[String],
    parent: Option[String],
    key: String,
    header: String,
    from: String,
    select: String,
    where: Option[String],
    join: Option[String],
    groupBy: Option[String],
    having: Option[String],
    orderBy: Option[String]
)

final case class DomainSchemaAttribute(
    vertexName: String,
    attributeName: String,
    attributeValue: String,
    attributeType: String,
    title: String
)

final case class DomainSchemaEdge(
    from: String,
    to: String,
    fromKey: String,
    toKey: String
)

object DomainSchemaVertex {
  implicit val config: Configuration              = Configuration.default.withSnakeCaseMemberNames
  implicit val codec: Codec[DomainSchemaVertex]   = deriveConfiguredCodec
  implicit val schema: Schema[DomainSchemaVertex] = Schema.derived
}

object DomainSchemaDTO {
  implicit val config: Configuration                        = Configuration.default.withSnakeCaseMemberNames
  implicit val attributeCodec: Codec[DomainSchemaAttribute] = deriveConfiguredCodec
  implicit val edgeCodec: Codec[DomainSchemaEdge]           = deriveConfiguredCodec
  implicit val codec: Codec[DomainSchemaDTO]                = deriveConfiguredCodec
}
