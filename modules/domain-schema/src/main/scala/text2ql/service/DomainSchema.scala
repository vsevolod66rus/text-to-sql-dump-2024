package text2ql.service

import cats.effect._
import cats.implicits._
import io.circe.yaml.{parser => yamlParser}
import text2ql.api.Domain
import text2ql.domainschema.{DomainSchemaColumn, DomainSchemaDTO, DomainSchemaRelation, DomainSchemaTable}
import text2ql.error.ServerError.ServerErrorWithMessage
import text2ql.service.DomainSchema.DomainSchemaInternal

trait DomainSchema[F[_]] {
  def update(domainSchema: String): F[Unit]
  def tables: F[Map[String, DomainSchemaTable]]
  def relations: F[List[DomainSchemaRelation]]
  def types: F[Map[String, String]]
  def titles: F[Map[String, String]]
  def sqlNames: F[Map[String, String]]
  def tableKeys: F[Map[String, String]]
  def tableColumns: F[Map[String, Set[String]]]
}

object DomainSchema {

  def apply[F[_]: Async](domain: Domain): Resource[F, DomainSchema[F]] =
    Resource.eval {
      for {
        raw              <- "".pure[F] // from db
        maybeJson         = yamlParser.parse(raw).some
        maybeDomainSchema = maybeJson.flatMap(_.toOption.flatMap(_.as[DomainSchemaDTO].toOption))
        domainSchema      = maybeDomainSchema.map(new DomainSchemaInternal(_))
        result           <- Ref.of[F, Option[DomainSchemaInternal]](domainSchema).map(new DomainSchemaImpl(_, domain))
      } yield result
    }

  final class DomainSchemaInternal(domainSchemaDTO: DomainSchemaDTO) {

    lazy val tables: Map[String, DomainSchemaTable] = domainSchemaDTO.tables.map(v => v.tableName -> v).toMap

    lazy val columns: Seq[DomainSchemaColumn] = domainSchemaDTO.columns

    lazy val columnsTypeMap: Map[String, String] = makeMapFromColumns(a => a.columnName -> a.columnType)

    lazy val titlesMap: Map[String, String] =
      makeMapFromColumns(a => a.columnName -> a.russianNames.headOption.getOrElse(a.columnName)) ++
        makeMapFromTables(th => th.tableName -> th.russianNames.headOption.getOrElse(th.tableName))

    lazy val sqlNames: Map[String, String]  = makeMapFromColumns(a => a.columnName -> a.columnValue)

    lazy val relations: List[DomainSchemaRelation] = domainSchemaDTO.relations

    lazy val tableKeysAliases: Map[String, String] = makeMapFromTables(th =>
      th.tableName -> domainSchemaDTO.columns
        .filter(_.tableName == th.tableName)
        .find(_.columnValue == th.key)
        .map(_.columnName)
        .getOrElse(th.key)
    )

    lazy val tableColumnNames: Map[String, Set[String]] =
      domainSchemaDTO.columns.groupBy(_.tableName).map { case (thing, attrs) =>
        thing -> attrs.map(_.columnName).toSet
      }

    private def makeMapFromTables[K, V](f: DomainSchemaTable => (K, V)): Map[K, V] =
      domainSchemaDTO.tables.map(f).toMap

    private def makeMapFromColumns[K, V](f: DomainSchemaColumn => (K, V)): Map[K, V] =
      domainSchemaDTO.columns.map(f).toMap

  }
}

final class DomainSchemaImpl[F[_]: Async](domainSchemaRef: Ref[F, Option[DomainSchemaInternal]], domain: Domain)
    extends DomainSchema[F] {

  private val domainSchema = for {
    maybeDomainSchema <- domainSchemaRef.get
    domainSchema      <- Async[F].fromOption(maybeDomainSchema, ServerErrorWithMessage(s" no schema for $domain"))
  } yield domainSchema

  override def update(domainSchemaYaml: String): F[Unit] = for {
    json            <- Async[F].delay(yamlParser.parse(domainSchemaYaml))
    newDomainSchema <- Async[F].delay(json.flatMap(_.as[DomainSchemaDTO]).fold(throw _, identity))
    _               <- domainSchemaRef.set(Some(new DomainSchemaInternal(newDomainSchema)))
  } yield ()

  override val tables: F[Map[String, DomainSchemaTable]] = domainSchema.map(_.tables)
  override val types: F[Map[String, String]]             = domainSchema.map(_.columnsTypeMap)
  override val titles: F[Map[String, String]]            = domainSchema.map(_.titlesMap)
  override val sqlNames: F[Map[String, String]]          = domainSchema.map(_.sqlNames)
  override val relations: F[List[DomainSchemaRelation]]  = domainSchema.map(_.relations)
  override val tableKeys: F[Map[String, String]]         = domainSchema.map(_.tableKeysAliases)
  override val tableColumns: F[Map[String, Set[String]]] = domainSchema.map(_.tableColumnNames)
}
