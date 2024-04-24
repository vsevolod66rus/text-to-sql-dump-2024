package text2ql.dao.postgres

import cats.effect.kernel.{Resource, Sync}
import cats.implicits._
import text2ql.api._
import text2ql.configs.DBDataConfig
import text2ql.domainschema.DomainSchemaRelation
import text2ql.service.DomainSchemaService

import scala.annotation.tailrec

trait QueryBuilder[F[_]] {
  def buildConstantSqlChunk(queryData: DataForDBQuery): F[String]
  def buildGeneralSqlQuery(queryData: DataForDBQuery, constantSqlChunk: String): F[BuildQueryDTO]
}

object QueryBuilder {

  def apply[F[_]: Sync](
      domainSchema: DomainSchemaService[F],
      dataConf: DBDataConfig
  ): Resource[F, QueryBuilder[F]] =
    Resource.eval(Sync[F].delay(new QueryBuilderImpl(domainSchema, dataConf)))
}

class QueryBuilderImpl[F[_]: Sync](
    domainSchema: DomainSchemaService[F],
    dataConf: DBDataConfig
) extends QueryBuilder[F] {

  override def buildConstantSqlChunk(queryData: DataForDBQuery): F[String] = for {
    edges       <- domainSchema.relations(queryData.domain)
    targetEntity = queryData.tables.find(_.isTargetEntity)
    query       <- targetEntity.fold("".pure[F])(e => bfs("".pure[F], List(e), Set.empty[String], queryData, edges))
    attributes   = queryData.tables.flatMap(e => e.attributes.map((e.tableName, _)))
    filters     <- attributes
                     .filter { case (_, attr) => attr.filterValues.nonEmpty }
                     .traverse { case (thing, attr) => buildFiltersChunk(queryData.domain, thing)(attr) }
    filtersChunk =
      filters.filter(_.nonEmpty).mkString(" and ").some.filter(_.nonEmpty).map("where " + _).fold("")(identity)
  } yield List("from", query, filtersChunk).mkString(" ")

  def buildGeneralSqlQuery(queryData: DataForDBQuery, constantSqlChunk: String): F[BuildQueryDTO] =
    for {
      visualization          <- queryData.properties.visualization.pure[F]
      attributesForMainSelect =
        queryData.tables
          .flatMap(e => e.attributes.map((e.tableName, _)))
          .filter { case (_, attribute) => visualization.contains(attribute.tableName) }
      mainSelect             <- collectMainSelect(attributesForMainSelect, queryData.domain)
      orderBy                 = buildOrderByChunk(queryData)
      query                   = s"$mainSelect $constantSqlChunk $orderBy"
      generalQuery            = withPagination(query, queryData.pagination)
      countTargetChunk       <- buildCountTargetChunk(queryData)
      countQuery              = s"select count(*), $countTargetChunk $constantSqlChunk"
    } yield BuildQueryDTO(
      generalQuery = generalQuery,
      countQuery = countQuery
    )

  private def withPagination(query: String, pagination: Option[ChatMessageRequestModel]): String = {

    def withCursorBasedPagination(offset: Int, limit: Int): String =
      s"select * from (select row_number() over() as cursor, * from (select $query)" +
        s" as sorted_res) as res where res.cursor > $offset and res.cursor < ${offset + limit + 1};"

    def withOffsetLimit(offset: Int, limit: Int): String = s"select $query limit $limit offset $offset;"

    val limitOpt  = pagination.flatMap(_.perPage)
    val offsetOpt = pagination.flatMap(_.page).flatMap(o => limitOpt.map(_ * o))
    (offsetOpt, limitOpt) match {
      case (Some(o), Some(l)) if dataConf.useCursor => withCursorBasedPagination(o, l)
      case (Some(o), Some(l))                       => withOffsetLimit(o, l)
      case _                                        => s"select $query;"
    }
  }

  private def buildOrderByChunk(queryData: DataForDBQuery): String =
    queryData.pagination
      .map(_.sort)
      .filter(_.orderBy.exists(_.nonEmpty))
      .filter(sortModel => sortModel.orderBy.exists(_.nonEmpty) && sortModel.direction.nonEmpty)
      .fold {
        val keyOpt = queryData.tables.find(_.isTargetEntity).map(e => s"${e.tableName}.${e.tableSchema.key}")
        keyOpt.fold("")(k => s"order by $k asc")
      } { sortModel =>
        s"order by ${sortModel.orderBy.getOrElse("orderBy")} ${sortModel.direction.getOrElse("sortDirection")}"
      }

  private def buildCountTargetChunk(queryData: DataForDBQuery): F[String] = {
    val targetThing = queryData.properties.targetThing
    for {
      distinctKey  <- domainSchema.thingKeys(queryData.domain).map(_.getOrElse(targetThing, targetThing))
      sqlNameKey   <- domainSchema.sqlNames(queryData.domain, distinctKey)
      distinctThing = s"${targetThing.toUpperCase}.$sqlNameKey"
    } yield s"count(distinct $distinctThing)"
  }

  private def collectMainSelect(attributes: List[(String, DBQueryColumn)], domain: Domain): F[String] =
    attributes
      .traverse { case (entityName, attr) =>
        domainSchema
          .sqlNames(domain, attr.tableName)
          .map(name => s"$entityName.$name as ${attr.tableName}")
      }
      .map(_.mkString(", "))

  private def buildThingChunk(
                               thing: DBQueryTable,
                               domain: Domain
  ): F[String] = for {
    selectChunk <- ("select " + thing.tableSchema.select).pure[F]
    pgSchemaName = dataConf.pgSchemas.getOrElse(domain, domain.entryName.toLowerCase)
    fromChunk    = s"from $pgSchemaName.${thing.tableSchema.from}"
    res          = List(
                     "(".some,
                     selectChunk.some,
                     fromChunk.some,
                     ") as".some,
                     thing.tableName.some
                   ).collect { case Some(s) => s }.mkString(" ")
  } yield res

  private def buildFiltersChunk(domain: Domain, thingName: String)(attributeQuery: DBQueryColumn): F[String] =
    for {
      name        <- domainSchema
                       .sqlNames(domain, attributeQuery.tableName)
                       .map(name => s"${thingName.toUpperCase}.$name")
      eitherOrAnd <- Sync[F].delay {
                       val isOrExists = attributeQuery.filterValues.headOption.exists(_.joinValuesWithOr)
                       if (isOrExists) "or" else "and"
                     }
      attrs       <- attributeQuery.filterValues
                       .traverse(buildAttributeFilter(domain, attributeQuery, name))
                       .map(_.filter(_.nonEmpty))
      res          = if (attrs.size > 1) {
                       val chunk = attrs.mkString(s") $eitherOrAnd (")
                       s"(($chunk))"
                     } else attrs.mkString(s" $eitherOrAnd ")
    } yield res

  private def buildAttributeFilter(domain: Domain, attributeQuery: DBQueryColumn, sqlNameRaw: String)(
      attribute: DbQueryFilterValue
  ): F[String] = for {
    attrsTypes <- domainSchema.schemaAttributesType(domain)
    attrType    = attrsTypes.getOrElse(attributeQuery.tableName, "string")
    sqlName     = if (attrType == "datetime") s"$sqlNameRaw::date" else sqlNameRaw
    value       = if (List("string", "datetime").contains(attrType)) s"'${attribute.value}'" else s"${attribute.value}"
    chunk       = s"$sqlName ${attribute.comparisonOperator} $value"
  } yield chunk

  @tailrec
  private def bfs(
                   acc: F[String],
                   queue: List[DBQueryTable],
                   visited: Set[String],
                   queryData: DataForDBQuery,
                   edges: List[DomainSchemaRelation]
  ): F[String] = queue match {
    case head :: tail =>
      val adjacentVertices = edges.collect {
        case e if e.from == head.tableName => e.to
        case e if e.to == head.tableName   => e.from
      }
      val sqlF             = for {
        currentSql  <- acc
        vertexChunk <- buildThingChunk(head, queryData.domain)
        joinOnChunk  = edges
                         .filter(e => Set(e.from, e.to).contains(head.tableName))
                         .filter(e => Set(e.from, e.to).intersect(visited).nonEmpty)
                         .map(e => s"${e.from}.${e.fromKey} = ${e.to}.${e.toKey}")
                         .mkString(" and ")
                         .some
                         .filter(_.nonEmpty)
                         .fold("")(ch => s"on $ch")
        res          =
          if (currentSql.isEmpty) s"$vertexChunk $joinOnChunk" else s"$currentSql inner join $vertexChunk $joinOnChunk"
      } yield res
      bfs(
        sqlF,
        tail ++ queryData.tables
          .filter(e => adjacentVertices.contains(e.tableName))
          .filterNot(e => visited.contains(e.tableName))
          .filterNot(e => tail.map(_.tableName).contains(e.tableName)),
        visited + head.tableName,
        queryData,
        edges
      )
    case _            => acc
  }
}
