package text2ql.domainschema

import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import sttp.tapir.Schema

final case class DomainSchemaDTO(
    tables: List[DomainSchemaTable],
    relations: List[DomainSchemaRelation],
    columns: List[DomainSchemaColumn]
)

final case class DomainSchemaTable(
    tableName: String,
    russianNames: List[String],
    key: String,
    header: String,
    from: String,
    select: String
)

final case class DomainSchemaColumn(
    tableName: String,
    columnName: String,
    columnValue: String,
    columnType: String,
    russianNames: List[String]
)

final case class DomainSchemaRelation(
    from: String,
    to: String,
    fromKey: String,
    toKey: String
)

object DomainSchemaTable {
  implicit val config: Configuration             = Configuration.default.withSnakeCaseMemberNames
  implicit val codec: Codec[DomainSchemaTable]   = deriveConfiguredCodec
  implicit val schema: Schema[DomainSchemaTable] = Schema.derived
}

object DomainSchemaDTO {
  implicit val config: Configuration                     = Configuration.default.withSnakeCaseMemberNames
  implicit val attributeCodec: Codec[DomainSchemaColumn] = deriveConfiguredCodec
  implicit val edgeCodec: Codec[DomainSchemaRelation]    = deriveConfiguredCodec
  implicit val codec: Codec[DomainSchemaDTO]             = deriveConfiguredCodec
}
