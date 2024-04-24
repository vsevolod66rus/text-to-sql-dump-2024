package text2ql.api

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import sttp.tapir.Schema
import text2ql.domainschema.DomainSchemaTable

import java.util.UUID

case class DataForDBQuery(
    requestId: UUID,
    domain: Domain,
    tables: List[DBQueryTable],
    pagination: Option[ChatMessageRequestModel] = None,
    properties: DBQueryProperties
)

case class DBQueryTable(
    tableName: String,
    tableSchema: DomainSchemaTable,
    attributes: List[DBQueryColumn],
    isTargetEntity: Boolean = false
)

case class DBQueryColumn(
    tableName: String,
    filterValues: List[DbQueryFilterValue],
    isTargetAttribute: Boolean = false
)

case class DbQueryFilterValue(
    value: String,
    comparisonOperator: String,
    joinValuesWithOr: Boolean = true
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

object DBQueryTable {
  implicit val codec: Codec[DBQueryTable]   = deriveCodec
  implicit val schema: Schema[DBQueryTable] = Schema.derived
}

object DBQueryColumn {
  implicit val codec: Codec[DBQueryColumn]   = deriveCodec
  implicit val schema: Schema[DBQueryColumn] = Schema.derived
}

object DbQueryFilterValue {
  implicit val codec: Codec[DbQueryFilterValue]   = deriveCodec
  implicit val schema: Schema[DbQueryFilterValue] = Schema.derived
}

object DBQueryProperties {
  implicit val codec: Codec[DBQueryProperties]   = deriveCodec
  implicit val schema: Schema[DBQueryProperties] = Schema.derived
}
