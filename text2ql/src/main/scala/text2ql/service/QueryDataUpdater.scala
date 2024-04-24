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
      case E_TYPE => updateDataForQueryWithEntity(currentDataForQuery, enrichedEntities)
      case A_TYPE => updateDataForQueryWithAttributeType(currentDataForQuery, enrichedEntities)
      case _      => updateDataForQueryWithAttribute(currentDataForQuery, enrichedEntities, slotName)
    }

  private def updateDataForQueryWithEntity(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity]
  ): F[DataForDBQuery] = enrichedEntities
    .filter(_.tag == E_TYPE)
    .foldLeftM(currentDataForQuery)((acc, el) => chunkUpdateDataForQueryWithEntity(acc, enrichedEntities, el))

  private def updateDataForQueryWithAttribute(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity],
      slotName: String
  ): F[DataForDBQuery] = {
    val domain = currentDataForQuery.domain

    def collectAttributeValues(clarifiedEntity: EnrichedNamedEntity): F[DbQueryFilterValue] =
      domainSchema.schemaAttributesType(domain).map { attrs =>
        val comparisonOperator = getComparisonOperator(enrichedEntities, clarifiedEntity)
        val joinValuesWithOr   = !attrs.get(slotName).contains("datetime")
        DbQueryFilterValue(clarifiedEntity.value, comparisonOperator, joinValuesWithOr = joinValuesWithOr)
      }

    for {
      entities        <- enrichedEntities.filter(_.tag == slotName).pure[F]
      attributeValues <- entities.traverse(collectAttributeValues)
      attributes       = entities.map { e =>
                           val isTargetRoleRole = e.isTarget
                           DBQueryColumn(
                             tableName = slotName,
                             filterValues = attributeValues,
                             isTargetAttribute = isTargetRoleRole
                           )
                         }
      res             <- attributes.foldLeftM(currentDataForQuery) { (acc, el) =>
                           chunkUpdateDataForQueryWithAttributeType(acc, el)
                         }
    } yield res
  }

  private def updateDataForQueryWithAttributeType(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity]
  ): F[DataForDBQuery] = {
    def collectAttributeForQuery(
        attributeTypeEntity: EnrichedNamedEntity
    ): List[DBQueryColumn] =
      enrichedEntities
        .filter(_.filterByTagAndValueOpt(A_TYPE, attributeTypeEntity.token.some))
        .map(_.value)
        .map { name =>
          val ownEntities        = enrichedEntities.view
            .filter(_.tag == "attribute_value")
            .filter(_.filterByGroupOpt(attributeTypeEntity.group))
          val isTargetRoleRole   = attributeTypeEntity.isTarget
          val comparisonOperator = getComparisonOperator(enrichedEntities, attributeTypeEntity)
          DBQueryColumn(
            tableName = name,
            filterValues = ownEntities.map(_.token).toList.map(DbQueryFilterValue(_, comparisonOperator)),
            isTargetAttribute = isTargetRoleRole
          )
        }

    for {
      attributesFromAttributeTypeSlot <- enrichedEntities
                                           .filter(_.tag == A_TYPE)
                                           .flatMap(collectAttributeForQuery)
                                           .pure[F]
      res                             <- attributesFromAttributeTypeSlot.foldLeftM(currentDataForQuery)(chunkUpdateDataForQueryWithAttributeType)
    } yield res
  }

  private def chunkUpdateDataForQueryWithEntity(
      currentDataForQuery: DataForDBQuery,
      enrichedEntities: List[EnrichedNamedEntity],
      entity: EnrichedNamedEntity
  ): F[DataForDBQuery] = for {
    entityName            <- entity.value.pure[F]
    entityAttributesNames <- domainSchema.getColumnsByTable(currentDataForQuery.domain)(entityName)
    isTargetRole           = enrichedEntities
                               .filter(_.isTarget)
                               .exists(_.filterByTagAndValueOpt(E_TYPE, entity.token.some))
    currentEntityInDataOpt = currentDataForQuery.tables.find(_.tableName == entityName)
    restEntities           = currentDataForQuery.tables.filter(_.tableName != entityName)
    entitySchemaOpt       <- domainSchema.vertices(currentDataForQuery.domain).map(_.get(entityName))
    entitySchema          <- Async[F].fromOption(entitySchemaOpt, ServerErrorWithMessage("no schema for vertex"))
    res                   <- isTargetRole
                               .pure[F]
                               .ifM(
                                 for {
                                   entityAttributes <- entityAttributesNames
                                                         .map(a => DBQueryColumn(tableName = a, filterValues = List.empty))
                                                         .pure[F]
                                   newEntity         = currentEntityInDataOpt.fold(
                                                         DBQueryTable(
                                                           tableName = entityName,
                                                           tableSchema = entitySchema,
                                                           attributes = entityAttributes,
                                                           isTargetEntity = isTargetRole
                                                         )
                                                       )(e => e.copy(isTargetEntity = isTargetRole))
                                   res              <-
                                     currentDataForQuery
                                       .copy(tables = restEntities.filterNot(_.tableName == newEntity.tableName) :+ newEntity)
                                       .pure[F]
                                 } yield res,
                                 entityAttributesNames
                                   .map { a =>
                                     DBQueryColumn(
                                       tableName = a,
                                       filterValues = List.empty
                                     )
                                   }
                                   .pure[F]
                                   .map { attrs =>
                                     currentDataForQuery
                                       .copy(tables =
                                         restEntities :+ currentEntityInDataOpt.fold(
                                           DBQueryTable(
                                             tableName = entityName,
                                             tableSchema = entitySchema,
                                             attributes = attrs
                                           )
                                         )(identity)
                                       )
                                   }
                               )
  } yield res

  private def chunkUpdateDataForQueryWithAttributeType(
      currentDataForQuery: DataForDBQuery,
      filterFromAttributeType: DBQueryColumn
  ): F[DataForDBQuery] = for {
    domain               <- currentDataForQuery.domain.pure[F]
    entityOrRelationName <- domainSchema.getTableByColumn(domain)(filterFromAttributeType.tableName)
    restAttributes       <- filterFromAttributeType.isTargetAttribute
                              .pure[F]
                              .ifM(
                                for {
                                  attrs <- domainSchema
                                             .getColumnsByTable(domain)(entityOrRelationName)
                                             .map(_.filter(_ != filterFromAttributeType.tableName))
                                  res    = attrs.map { a =>
                                             DBQueryColumn(
                                               tableName = a,
                                               filterValues = List.empty
                                             )
                                           }
                                } yield res,
                                List.empty[DBQueryColumn].pure[F]
                              )
    entityName            = entityOrRelationName
    res                  <- for {
                              entitySchemaOpt <- domainSchema.vertices(domain).map(_.get(entityName))
                              entitySchema    <- Async[F].fromOption(entitySchemaOpt, ServerErrorWithMessage("no schema for vertex"))
                              res             <- currentDataForQuery.pure[F].map { data =>
                                                   val entityAlreadyInDataOpt = data.tables.find(_.tableName == entityName)
                                                   val updatedEntity          = entityAlreadyInDataOpt
                                                     .fold(
                                                       DBQueryTable(
                                                         tableName = entityName,
                                                         tableSchema = entitySchema,
                                                         attributes = filterFromAttributeType +: restAttributes,
                                                         isTargetEntity = filterFromAttributeType.isTargetAttribute
                                                       )
                                                     ) { e =>
                                                       e.copy(
                                                         attributes = e.attributes.filterNot(
                                                           _.tableName == filterFromAttributeType.tableName
                                                         ) :+ filterFromAttributeType,
                                                         isTargetEntity = filterFromAttributeType.isTargetAttribute || e.isTargetEntity
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
