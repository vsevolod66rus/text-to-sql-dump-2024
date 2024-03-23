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
      clarifiedEntities: List[ClarifiedNamedEntity],
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
      clarifiedEntities: List[ClarifiedNamedEntity],
      domain: Domain,
      slotName: String
  ): F[DataForDBQuery] =
    slotName match {
      case R_TYPE => currentDataForQuery.pure[F]
      case E_TYPE => updateDataForQueryWithEntity(currentDataForQuery, clarifiedEntities)
      case A_TYPE => updateDataForQueryWithAttributeType(currentDataForQuery, clarifiedEntities)
      case _      => updateDataForQueryWithAttribute(currentDataForQuery, clarifiedEntities, slotName)
    }

  private def updateDataForQueryWithEntity(
      currentDataForQuery: DataForDBQuery,
      clarifiedEntities: List[ClarifiedNamedEntity]
  ): F[DataForDBQuery] = for {
    entitiesFromSlot <-
      clarifiedEntities.view
        .filter(_.tag == E_TYPE)
        .map(e => ThingWithOriginalName(e.originalValue, e.namedValues.headOption.getOrElse(e.originalValue)))
        .toList
        .pure[F]
    res              <- entitiesFromSlot.foldLeftM(currentDataForQuery)((acc, el) =>
                          chunkUpdateDataForQueryWithEntity(acc, clarifiedEntities, el)
                        )
  } yield res

  private def updateDataForQueryWithAttribute(
      currentDataForQuery: DataForDBQuery,
      clarifiedEntities: List[ClarifiedNamedEntity],
      slotName: String
  ): F[DataForDBQuery] = {
    val domain = currentDataForQuery.domain

    def collectAttributeValues(clarifiedEntity: ClarifiedNamedEntity): F[List[AttributeValue]] =
      domainSchema.schemaAttributesType(domain).map { attrs =>
        val comparisonOperator = getComparisonOperator(clarifiedEntities, clarifiedEntity)
        val joinValuesWithOr   = !attrs.get(slotName).contains("datetime")
        clarifiedEntity.namedValues.map(AttributeValue(_, comparisonOperator, joinValuesWithOr = joinValuesWithOr))
      }

    for {
      entities        <- clarifiedEntities.filter(_.tag == slotName).pure[F]
      attributeValues <- entities.traverse(collectAttributeValues).map(_.flatten)
      attributes       = entities.map { e =>
                           val isTargetRoleRole = e.isTarget
                           AttributeForDBQuery(
                             attributeName = slotName,
                             attributeValues = attributeValues,
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
      clarifiedEntities: List[ClarifiedNamedEntity]
  ): F[DataForDBQuery] = {
    def collectAttributeForQuery(
        attributeTypeEntity: ClarifiedNamedEntity
    ): List[AttributeForDBQuery] =
      clarifiedEntities
        .filter(_.filterByTagAndValueOpt(A_TYPE, attributeTypeEntity.originalValue.some))
        .flatMap(_.namedValues)
        .map { name =>
          val ownEntities        = clarifiedEntities.view
            .filter(_.tag == "attribute_value")
            .filter(_.filterByGroupOpt(attributeTypeEntity.group))
          val isTargetRoleRole   = attributeTypeEntity.isTarget
          val comparisonOperator = getComparisonOperator(clarifiedEntities, attributeTypeEntity)
          AttributeForDBQuery(
            attributeName = name,
            attributeValues = ownEntities.map(_.originalValue).toList.map(AttributeValue(_, comparisonOperator)),
            isTargetAttribute = isTargetRoleRole
          )
        }

    for {
      attributesFromAttributeTypeSlot <- clarifiedEntities
                                           .filter(_.tag == A_TYPE)
                                           .flatMap(collectAttributeForQuery)
                                           .pure[F]
      res                             <- attributesFromAttributeTypeSlot.foldLeftM(currentDataForQuery)(chunkUpdateDataForQueryWithAttributeType)
    } yield res
  }

  private def chunkUpdateDataForQueryWithEntity(
      currentDataForQuery: DataForDBQuery,
      clarifiedEntities: List[ClarifiedNamedEntity],
      entity: ThingWithOriginalName
  ): F[DataForDBQuery] = for {
    entityName            <- entity.thingName.pure[F]
    entityAttributesNames <- domainSchema.getAttributesByThing(currentDataForQuery.domain)(entityName)
    isTargetRole           = clarifiedEntities
                               .filter(_.isTarget)
                               .exists(_.filterByTagAndValueOpt(E_TYPE, entity.originalName.some))
    currentEntityInDataOpt = currentDataForQuery.entityList.find(_.entityName == entityName)
    restEntities           = currentDataForQuery.entityList.filter(_.entityName != entityName)
    entitySchemaOpt       <- domainSchema.vertices(currentDataForQuery.domain).map(_.get(entityName))
    entitySchema          <- Async[F].fromOption(entitySchemaOpt, ServerErrorWithMessage("no schema for vertex"))
    res                   <- isTargetRole
                               .pure[F]
                               .ifM(
                                 for {
                                   entityAttributes <- entityAttributesNames.map(a => AttributeForDBQuery(attributeName = a)).pure[F]
                                   newEntity         = currentEntityInDataOpt.fold(
                                                         EntityForDBQuery(
                                                           entityName = entityName,
                                                           schema = entitySchema,
                                                           attributes = entityAttributes,
                                                           isTargetEntity = isTargetRole
                                                         )
                                                       )(e => e.copy(isTargetEntity = isTargetRole))
                                   res              <-
                                     currentDataForQuery
                                       .copy(entityList = restEntities.filterNot(_.entityName == newEntity.entityName) :+ newEntity)
                                       .pure[F]
                                 } yield res,
                                 entityAttributesNames
                                   .map { a =>
                                     AttributeForDBQuery(
                                       attributeName = a
                                     )
                                   }
                                   .pure[F]
                                   .map { attrs =>
                                     currentDataForQuery
                                       .copy(entityList =
                                         restEntities :+ currentEntityInDataOpt.fold(
                                           EntityForDBQuery(
                                             entityName = entityName,
                                             schema = entitySchema,
                                             attributes = attrs
                                           )
                                         )(identity)
                                       )
                                   }
                               )
  } yield res

  private def chunkUpdateDataForQueryWithAttributeType(
      currentDataForQuery: DataForDBQuery,
      filterFromAttributeType: AttributeForDBQuery
  ): F[DataForDBQuery] = for {
    domain               <- currentDataForQuery.domain.pure[F]
    entityOrRelationName <- domainSchema.getThingByAttribute(domain)(filterFromAttributeType.attributeName)
    restAttributes       <- filterFromAttributeType.isTargetAttribute
                              .pure[F]
                              .ifM(
                                for {
                                  attrs <- domainSchema
                                             .getAttributesByThing(domain)(entityOrRelationName)
                                             .map(_.filter(_ != filterFromAttributeType.attributeName))
                                  res    = attrs.map { a =>
                                             AttributeForDBQuery(
                                               attributeName = a
                                             )
                                           }
                                } yield res,
                                List.empty[AttributeForDBQuery].pure[F]
                              )
    entityName            = entityOrRelationName
    res                  <- for {
                              entitySchemaOpt <- domainSchema.vertices(domain).map(_.get(entityName))
                              entitySchema    <- Async[F].fromOption(entitySchemaOpt, ServerErrorWithMessage("no schema for vertex"))
                              res             <- currentDataForQuery.pure[F].map { data =>
                                                   val entityAlreadyInDataOpt = data.entityList.find(_.entityName == entityName)
                                                   val updatedEntity          = entityAlreadyInDataOpt
                                                     .fold(
                                                       EntityForDBQuery(
                                                         entityName = entityName,
                                                         schema = entitySchema,
                                                         attributes = filterFromAttributeType +: restAttributes,
                                                         isTargetEntity = filterFromAttributeType.isTargetAttribute
                                                       )
                                                     ) { e =>
                                                       e.copy(
                                                         attributes = e.attributes.filterNot(
                                                           _.attributeName == filterFromAttributeType.attributeName
                                                         ) :+ filterFromAttributeType,
                                                         isTargetEntity = filterFromAttributeType.isTargetAttribute || e.isTargetEntity
                                                       )
                                                     }
                                                   data.copy(entityList = data.entityList.filterNot {
                                                     _.entityName == updatedEntity.entityName
                                                   } :+ updatedEntity)
                                                 }
                            } yield res
  } yield res

  private def getComparisonOperator(
      clarifiedEntities: List[ClarifiedNamedEntity],
      coEntity: ClarifiedNamedEntity
  ): String = {
    val comparisonOperatorOriginal = clarifiedEntities.view
      .filter(_.tag == CO)
      .find(_.filterByGroupOpt(coEntity.group))
      .map(_.originalValue)
    clarifiedEntities.view
      .filter(_.filterByGroupOpt(coEntity.group))
      .find(_.filterByTagAndValueOpt(CO, comparisonOperatorOriginal))
      .flatMap(_.findFirstNamedValue)
      .getOrElse("=")
  }

}
