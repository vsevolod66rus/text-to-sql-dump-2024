package text2ql.service

import cats.effect.Async
import cats.effect.kernel.Resource
import cats.implicits._
import text2ql.api._
import text2ql.error.ServerError.ServerErrorWithMessage
import text2ql.service.DomainSchemaService._

trait QueryDataUpdater[F[_]] {

  def updateDataForDBQuery(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity],
      domain: Domain,
      slotName: String
  ): F[DataForDBQuery]

}

object QueryDataUpdater {

  def apply[F[+_]: Async](domainSchema: DomainSchemaService[F]): Resource[F, QueryDataUpdaterImpl[F]] =
    Resource.eval(Async[F].delay(new QueryDataUpdaterImpl(domainSchema)))
}

class QueryDataUpdaterImpl[F[+_]: Async](domainSchema: DomainSchemaService[F]) extends QueryDataUpdater[F] {

  override def updateDataForDBQuery(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity],
      domain: Domain,
      slotName: String
  ): F[DataForDBQuery] =
    slotName match {
      case TABLE  => updateDataForQueryWithTable(currentDataForQuery, enrichedEntities)
      case COLUMN => updateDataForQueryWithColumnType(currentDataForQuery, enrichedEntities)
      case _      => updateDataForQueryWithColumn(currentDataForQuery, enrichedEntities, slotName)
    }

  private def updateDataForQueryWithTable(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity]
  ): F[DataForDBQuery] = enrichedEntities
    .filter(_.tag == TABLE)
    .foldLeftM(currentDataForQuery)((acc, el) => chunkUpdateDataForQueryWithTable(acc, enrichedEntities, el))

  private def updateDataForQueryWithColumn(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity],
      slotName: String
  ): F[DataForDBQuery] = {
    val domain = currentDataForQuery.domain

    def collectColumnValues(clarifiedEntity: EnrichedNamedEntity): F[DbQueryFilterValue] =
      domainSchema.types(domain).map { attrs =>
        val comparisonOperator = getComparisonOperator(enrichedEntities, clarifiedEntity)
        val joinValuesWithOr   = !attrs.get(slotName).contains("datetime")
        DbQueryFilterValue(clarifiedEntity.value, comparisonOperator, joinValuesWithOr = joinValuesWithOr)
      }

    for {
      entities     <- enrichedEntities.filter(_.tag == slotName).pure[F]
      columnValues <- entities.traverse(collectColumnValues)
      columns       = entities.map { e =>
                        val isTargetRoleRole = e.isTarget
                        DBQueryColumn(
                          columnName = slotName,
                          filterValues = columnValues,
                          isTargetColumn = isTargetRoleRole
                        )
                      }
      res          <- columns.foldLeftM(currentDataForQuery) { (acc, el) =>
                        chunkUpdateDataForQueryWithColumnsType(acc, el)
                      }
    } yield res
  }

  private def updateDataForQueryWithColumnType(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity]
  ): F[DataForDBQuery] = {
    def collectColumnForQuery(
        columnTypeEntity: EnrichedNamedEntity
    ): List[DBQueryColumn] =
      enrichedEntities
        .filter(_.filterByTagAndValueOpt(COLUMN, columnTypeEntity.token.some))
        .map(_.value)
        .map { name =>
          val ownEntities        = enrichedEntities.view
            .filter(_.tag == "column_value")
            .filter(_.filterByGroupOpt(columnTypeEntity.group))
          val isTargetRoleRole   = columnTypeEntity.isTarget
          val comparisonOperator = getComparisonOperator(enrichedEntities, columnTypeEntity)
          DBQueryColumn(
            columnName = name,
            filterValues = ownEntities.map(_.token).toList.map(DbQueryFilterValue(_, comparisonOperator)),
            isTargetColumn = isTargetRoleRole
          )
        }

    for {
      columnsFromColumnTypeSlot <- enrichedEntities
                                     .filter(_.tag == COLUMN)
                                     .flatMap(collectColumnForQuery)
                                     .pure[F]
      res                       <- columnsFromColumnTypeSlot.foldLeftM(currentDataForQuery)(chunkUpdateDataForQueryWithColumnsType)
    } yield res
  }

  private def chunkUpdateDataForQueryWithTable(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity],
      entity: EnrichedNamedEntity
  ): F[DataForDBQuery] = for {
    tableName             <- entity.value.pure[F]
    tableColumnsNames     <- domainSchema.getColumnsByTable(currentDataForQuery.domain)(tableName)
    isTargetRole           = enrichedEntities
                               .filter(_.isTarget)
                               .exists(_.filterByTagAndValueOpt(TABLE, entity.token.some))
    currentEntityInDataOpt = currentDataForQuery.tables.find(_.tableName == tableName)
    restEntities           = currentDataForQuery.tables.filter(_.tableName != tableName)
    entitySchemaOpt       <- domainSchema.tables(currentDataForQuery.domain).map(_.get(tableName))
    entitySchema          <- Async[F].fromOption(entitySchemaOpt, ServerErrorWithMessage("no schema for vertex"))
    res                   <- isTargetRole
                               .pure[F]
                               .ifM(
                                 for {
                                   tableColumns <- tableColumnsNames
                                                     .map(a => DBQueryColumn(columnName = a, filterValues = List.empty))
                                                     .pure[F]
                                   newEntity     = currentEntityInDataOpt.fold(
                                                     DBQueryTable(
                                                       tableName = tableName,
                                                       tableSchema = entitySchema,
                                                       columns = tableColumns,
                                                       isTargetTable = isTargetRole
                                                     )
                                                   )(e => e.copy(isTargetTable = isTargetRole))
                                   res          <-
                                     currentDataForQuery
                                       .copy(tables = restEntities.filterNot(_.tableName == newEntity.tableName) :+ newEntity)
                                       .pure[F]
                                 } yield res,
                                 tableColumnsNames
                                   .map { a =>
                                     DBQueryColumn(
                                       columnName = a,
                                       filterValues = List.empty
                                     )
                                   }
                                   .pure[F]
                                   .map { attrs =>
                                     currentDataForQuery
                                       .copy(tables =
                                         restEntities :+ currentEntityInDataOpt.fold(
                                           DBQueryTable(
                                             tableName = tableName,
                                             tableSchema = entitySchema,
                                             columns = attrs
                                           )
                                         )(identity)
                                       )
                                   }
                               )
  } yield res

  private def chunkUpdateDataForQueryWithColumnsType(
      currentDataForQuery: DataForDBQuery,
      filterFromColumnsType: DBQueryColumn
  ): F[DataForDBQuery] = for {
    domain               <- currentDataForQuery.domain.pure[F]
    entityOrRelationName <- domainSchema.getTableByColumn(domain)(filterFromColumnsType.columnName)
    columns              <- filterFromColumnsType.isTargetColumn
                              .pure[F]
                              .ifM(
                                for {
                                  attrs <- domainSchema
                                             .getColumnsByTable(domain)(entityOrRelationName)
                                             .map(_.filter(_ != filterFromColumnsType.columnName))
                                  res    = attrs.map { a =>
                                             DBQueryColumn(
                                               columnName = a,
                                               filterValues = List.empty
                                             )
                                           }
                                } yield res,
                                List.empty[DBQueryColumn].pure[F]
                              )
    entityName            = entityOrRelationName
    res                  <- for {
                              entitySchemaOpt <- domainSchema.tables(domain).map(_.get(entityName))
                              entitySchema    <- Async[F].fromOption(entitySchemaOpt, ServerErrorWithMessage("no schema for vertex"))
                              res             <- currentDataForQuery.pure[F].map { data =>
                                                   val entityAlreadyInDataOpt = data.tables.find(_.tableName == entityName)
                                                   val updatedEntity          = entityAlreadyInDataOpt
                                                     .fold(
                                                       DBQueryTable(
                                                         tableName = entityName,
                                                         tableSchema = entitySchema,
                                                         columns = filterFromColumnsType +: columns,
                                                         isTargetTable = filterFromColumnsType.isTargetColumn
                                                       )
                                                     ) { e =>
                                                       e.copy(
                                                         columns = e.columns.filterNot(
                                                           _.columnName == filterFromColumnsType.columnName
                                                         ) :+ filterFromColumnsType,
                                                         isTargetTable = filterFromColumnsType.isTargetColumn || e.isTargetTable
                                                       )
                                                     }
                                                   data.copy(tables = data.tables.filterNot {
                                                     _.tableName == updatedEntity.tableName
                                                   } :+ updatedEntity)
                                                 }
                            } yield res
  } yield res

  private def getComparisonOperator(
      enrichedEntities: List[EnrichedNamedEntity],
      coEntity: EnrichedNamedEntity
  ): String = {
    val comparisonOperatorOriginal = enrichedEntities.view
      .filter(_.tag == CO)
      .find(_.filterByGroupOpt(coEntity.group))
      .map(_.token)
    enrichedEntities.view
      .filter(_.filterByGroupOpt(coEntity.group))
      .find(_.filterByTagAndValueOpt(CO, comparisonOperatorOriginal))
      .map(_.value)
      .getOrElse("=")
  }

}
