package text2ql.dao.typedb

import cats.effect.kernel.{Resource, Sync}
import cats.implicits._
import com.vaticle.typedb.client.api.answer.ConceptMap
import text2ql.api._
import text2ql.service.DomainSchemaService

trait TypeDBResponseBuilder[F[_]] {

  def collectAggregateClause(attributes: Seq[AttributeForDBQuery], domain: Domain): F[String]

  def makeGridProperties(
      queryData: DataForDBQuery,
      raw: Seq[ConceptMap]
  ): F[List[GridPropertyItemModel]]

  def makeGrid(
      queryData: DataForDBQuery,
      raw: Seq[ConceptMap],
      total: Long,
      domain: Domain,
      offset: Int,
      limit: Int
  ): F[Option[GridWithDataRenderTypeResponseModel]]

  def getCMAttributeValue(
      cm: ConceptMap,
      attribute: String,
      domain: Domain
  ): F[GridPropertyValue]

  def getCMAttributeStringValue(
      cm: ConceptMap,
      attribute: String
  ): String
}

object TypeDBResponseBuilder {

  def apply[F[_]: Sync](
      domainSchema: DomainSchemaService[F]
  ): Resource[F, TypeDBResponseBuilder[F]] =
    Resource.eval(Sync[F].delay(new TypeDBResponseBuilderImpl(domainSchema)))
}

class TypeDBResponseBuilderImpl[F[_]: Sync](domainSchema: DomainSchemaService[F]) extends TypeDBResponseBuilder[F] {

  override def collectAggregateClause(attributes: Seq[AttributeForDBQuery], domain: Domain): F[String] =
    attributes
      .filter(_.attributeValues.nonEmpty)
      .traverse { attribute =>
        for {
          attributeType <-
            domainSchema.schemaAttributesType(domain).map(_.getOrElse(attribute.attributeName, "string"))
          joinValuesPart = if (attribute.attributeValues.exists(_.joinValuesWithOr)) "or" else ";"
          clause         = attributeType match {
                             case "string" =>
                               val co = "="
                               attribute.attributeValues
                                 .map(el => s"{$$${attribute.attributeName} $co '${el.value}';}")
                                 .mkString("", s" $joinValuesPart ", ";")
                             case _        =>
                               attribute.attributeValues
                                 .map(a => if (attributeType == "boolean") a.copy(value = a.value.toLowerCase) else a)
                                 .map(el => s"{$$${attribute.attributeName} ${el.comparisonOperator} ${el.value};}")
                                 .mkString("", s" $joinValuesPart ", ";")

                           }
        } yield clause
      }
      .map(_.mkString)

  override def makeGridProperties(
      queryData: DataForDBQuery,
      raw: Seq[ConceptMap]
  ): F[List[GridPropertyItemModel]] =
    queryData.properties.visualization
      .traverse { attribute =>
        for {
          attrType <- domainSchema.schemaAttributesType(queryData.domain).map(_.getOrElse(attribute, "string"))
          title    <- domainSchema.attributesTitle(queryData.domain).map(_.getOrElse(attribute, attribute))
        } yield GridPropertyItemModel(
          key = attribute,
          title = title,
          dataType = GridPropertyDataType.fromType(attrType)
        )
      }

  override def makeGrid(
      queryData: DataForDBQuery,
      raw: Seq[ConceptMap],
      total: Long,
      domain: Domain,
      offset: Int,
      limit: Int
  ): F[Option[GridWithDataRenderTypeResponseModel]] = for {
    properties <- makeGridProperties(queryData, raw)

    result <- for {
                items <- raw.toList
                           .traverse { cm =>
                             properties
                               .traverse(prop => getCMAttributeValue(cm, prop.key, domain).map(a => prop.key -> a))
                               .map(_.groupMapReduce(_._1)(_._2)((_, v) => v))
                               .map { m =>
                                 Map("id" -> GridPropertyValueString(java.util.UUID.randomUUID().toString)) ++ m
                               }

                           }
              } yield GridWithDataRenderTypeResponseModel(
                properties = properties,
                items = items,
                total = total
              ).some
  } yield result

  override def getCMAttributeValue(
      cm: ConceptMap,
      attribute: String,
      domain: Domain
  ): F[GridPropertyValue] =
    domainSchema.schemaAttributesType(domain).map(_.getOrElse(attribute, "string")).map { attrType =>
      val stringValue = getCMAttributeStringValue(cm, attribute)
      GridPropertyValue.fromValueAndType(stringValue, attrType)
    }

  override def getCMAttributeStringValue(
      cm: ConceptMap,
      attribute: String
  ): String = cm.get(attribute).asAttribute().getValue.toString

}
