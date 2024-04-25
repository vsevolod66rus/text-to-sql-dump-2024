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
    properties: DBQueryProperties
)

case class DBQueryTable(
    tableName: String,
    tableSchema: DomainSchemaTable,
    columns: List[DBQueryColumn],
    isTargetTable: Boolean = false
)

case class DBQueryColumn(
    columnName: String,
    filterValues: List[DbQueryFilterValue],
    isTargetColumn: Boolean = false
)

case class DbQueryFilterValue(
    value: String,
    comparisonOperator: String,
    joinValuesWithOr: Boolean = true
)

case class DBQueryProperties(
    targetAttr: String,
    targetThing: String,
    sort: Option[BaseSortModel],
    visualization: List[String],
    page: Option[Int],
    perPage: Option[Int]
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
