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
    relations   <- domainSchema.relations(queryData.domain)
    targetTable  = queryData.tables.find(_.isTargetTable)
    query       <- targetTable.fold("".pure[F])(e => bfs("".pure[F], List(e), Set.empty[String], queryData, relations))
    columns      = queryData.tables.flatMap(e => e.columns.map((e.tableName, _)))
    filters     <- columns
                     .filter { case (_, col) => col.filterValues.nonEmpty }
                     .traverse { case (thing, col) => buildFiltersChunk(queryData.domain, thing)(col) }
    filtersChunk =
      filters.filter(_.nonEmpty).mkString(" and ").some.filter(_.nonEmpty).map("where " + _).fold("")(identity)
  } yield List("from", query, filtersChunk).mkString(" ")

  def buildGeneralSqlQuery(queryData: DataForDBQuery, constantSqlChunk: String): F[BuildQueryDTO] =
    for {
      visualization       <- queryData.properties.visualization.pure[F]
      columnsForMainSelect =
        queryData.tables
          .flatMap(e => e.columns.map((e.tableName, _)))
          .filter { case (_, column) => visualization.contains(column.columnName) }
      mainSelect          <- collectMainSelect(columnsForMainSelect, queryData.domain)
      orderBy              = buildOrderByChunk(queryData)
      query                = s"$mainSelect $constantSqlChunk $orderBy"
      generalQuery         = withPagination(query, queryData.properties.page, queryData.properties.perPage)
      countTargetChunk    <- buildCountTargetChunk(queryData)
      countQuery           = s"select count(*), $countTargetChunk $constantSqlChunk"
    } yield BuildQueryDTO(
      generalQuery = generalQuery,
      countQuery = countQuery
    )

  private def withPagination(query: String, page: Option[Int], perPage: Option[Int]): String = {

    def withCursorBasedPagination(offset: Int, limit: Int): String =
      s"select * from (select row_number() over() as cursor, * from (select $query)" +
        s" as sorted_res) as res where res.cursor > $offset and res.cursor < ${offset + limit + 1};"

    def withOffsetLimit(offset: Int, limit: Int): String = s"select $query limit $limit offset $offset;"

    val limitOpt  = perPage
    val offsetOpt = page.flatMap(o => limitOpt.map(_ * o))
    (offsetOpt, limitOpt) match {
      case (Some(o), Some(l)) if dataConf.useCursor => withCursorBasedPagination(o, l)
      case (Some(o), Some(l))                       => withOffsetLimit(o, l)
      case _                                        => s"select $query;"
    }
  }

  private def buildOrderByChunk(queryData: DataForDBQuery): String =
    queryData.properties.sort
      .filter(sortModel => sortModel.orderBy.exists(_.nonEmpty) && sortModel.direction.nonEmpty)
      .fold {
        val keyOpt = queryData.tables.find(_.isTargetTable).map(e => s"${e.tableName}.${e.tableSchema.key}")
        keyOpt.fold("")(k => s"order by $k asc")
      } { sortModel =>
        s"order by ${sortModel.orderBy.getOrElse("orderBy")} ${sortModel.direction.getOrElse("sortDirection")}"
      }

  private def buildCountTargetChunk(queryData: DataForDBQuery): F[String] = {
    val targetThing = queryData.properties.targetThing
    for {
      distinctKey  <- domainSchema.tableKeys(queryData.domain).map(_.getOrElse(targetThing, targetThing))
      sqlNameKey   <- domainSchema.sqlNames(queryData.domain, distinctKey)
      distinctThing = s"${targetThing.toUpperCase}.$sqlNameKey"
    } yield s"count(distinct $distinctThing)"
  }

  private def collectMainSelect(columns: List[(String, DBQueryColumn)], domain: Domain): F[String] =
    columns
      .traverse { case (tableName, column) =>
        domainSchema
          .sqlNames(domain, column.columnName)
          .map(name => s"$tableName.$name as ${column.columnName}")
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

  private def buildFiltersChunk(domain: Domain, tableName: String)(columnQuery: DBQueryColumn): F[String] =
    for {
      name        <- domainSchema
                       .sqlNames(domain, columnQuery.columnName)
                       .map(name => s"${tableName.toUpperCase}.$name")
      eitherOrAnd <- Sync[F].delay {
                       val isOrExists = columnQuery.filterValues.headOption.exists(_.joinValuesWithOr)
                       if (isOrExists) "or" else "and"
                     }
      columns     <- columnQuery.filterValues
                       .traverse(buildColumnFilter(domain, columnQuery, name))
                       .map(_.filter(_.nonEmpty))
      res          = if (columns.size > 1) {
                       val chunk = columns.mkString(s") $eitherOrAnd (")
                       s"(($chunk))"
                     } else columns.mkString(s" $eitherOrAnd ")
    } yield res

  private def buildColumnFilter(domain: Domain, columnQuery: DBQueryColumn, sqlNameRaw: String)(
      columns: DbQueryFilterValue
  ): F[String] =
    for {
      columnTypes <- domainSchema.types(domain)
      columnType   = columnTypes.getOrElse(columnQuery.columnName, "string")
      sqlName      = if (columnType == "datetime") s"$sqlNameRaw::date" else sqlNameRaw
      value        = if (List("string", "datetime").contains(columnType)) s"'${columns.value}'" else s"${columns.value}"
      chunk        = s"$sqlName ${columns.comparisonOperator} $value"
    } yield chunk

  @tailrec
  private def bfs(
      acc: F[String],
      queue: List[DBQueryTable],
      visited: Set[String],
      queryData: DataForDBQuery,
      relations: List[DomainSchemaRelation]
  ): F[String] = queue match {
    case head :: tail =>
      val adjacentVertices = relations.collect {
        case e if e.from == head.tableName => e.to
        case e if e.to == head.tableName   => e.from
      }

      val sqlF = for {
        currentSql  <- acc
        vertexChunk <- buildThingChunk(head, queryData.domain)
        joinOnChunk  = relations
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
        relations
      )
    case _            => acc
  }
}
