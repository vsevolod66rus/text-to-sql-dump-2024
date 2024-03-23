package text2ql.api

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema
import text2ql.domainschema.DomainSchemaVertex

import java.util.UUID

case class DataForDBQuery(
    requestId: UUID,
    domain: Domain,
    entityList: List[EntityForDBQuery],
    relationList: List[RelationForDBQuery],
    pagination: Option[ChatMessageRequestModel] = None,
    properties: DBQueryProperties
)

case class EntityForDBQuery(
    entityName: String,
    schema: DomainSchemaVertex,
    attributes: List[AttributeForDBQuery] = List.empty[AttributeForDBQuery],
    isTargetEntity: Boolean = false
)

case class RelationForDBQuery(
    relationName: String,
    attributes: List[AttributeForDBQuery] = List.empty[AttributeForDBQuery],
    entities: List[String]
)

case class AttributeForDBQuery(
    attributeName: String,
    attributeValues: List[AttributeValue] = List.empty,
    isTargetAttribute: Boolean = false
)

case class AttributeValue(
    value: String,
    comparisonOperator: String,
    joinValuesWithOr: Boolean = true
)

case class ThingWithOriginalName(
    originalName: String,
    thingName: String
)

case class DBQueryProperties(
    targetAttr: String,
    targetThing: String,
    sortModelOpt: Option[BaseSortModel],
    visualization: List[String]
)

object DataForDBQuery {
  implicit val codec: Codec[DataForDBQuery]   = deriveCodec
  implicit val schema: Schema[DataForDBQuery] = Schema.derived
}

object EntityForDBQuery {
  implicit val codec: Codec[EntityForDBQuery]   = deriveCodec
  implicit val schema: Schema[EntityForDBQuery] = Schema.derived
}

object RelationForDBQuery {
  implicit val codec: Codec[RelationForDBQuery]   = deriveCodec
  implicit val schema: Schema[RelationForDBQuery] = Schema.derived
}

object AttributeForDBQuery {
  implicit val codec: Codec[AttributeForDBQuery]   = deriveCodec
  implicit val schema: Schema[AttributeForDBQuery] = Schema.derived
}

object AttributeValue {
  implicit val codec: Codec[AttributeValue]   = deriveCodec
  implicit val schema: Schema[AttributeValue] = Schema.derived
}

object DBQueryProperties {
  implicit val codec: Codec[DBQueryProperties]   = deriveCodec
  implicit val schema: Schema[DBQueryProperties] = Schema.derived
}
