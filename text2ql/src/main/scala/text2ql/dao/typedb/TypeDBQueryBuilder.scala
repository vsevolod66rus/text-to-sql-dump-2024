package text2ql.dao.typedb

import cats.effect.kernel._
import cats.implicits._
import text2ql.api.{DBQueryProperties, DataForDBQuery, Domain, RelationForDBQuery}
import text2ql.service.DomainSchemaService
import text2ql.service.DomainSchemaService.relationsMap

trait TypeDBQueryBuilder[F[_]] {

  def build(
      queryData: DataForDBQuery,
      logic: DBQueryProperties,
      unlimitedQuery: Boolean,
      configLimit: Int,
      domain: Domain,
      countQuery: Boolean = false,
      countDistinctQuery: Boolean = false
  ): F[String]

}

object TypeDBQueryBuilder {

  def apply[F[_]: Sync](
      queryHelper: TypeDBResponseBuilder[F],
      domainSchema: DomainSchemaService[F]
  ): Resource[F, TypeDBQueryBuilder[F]] =
    Resource.eval(Sync[F].delay(new TypeDBQueryBuilderImpl(queryHelper, domainSchema)))

}

class TypeDBQueryBuilderImpl[F[_]: Sync](queryHelper: TypeDBResponseBuilder[F], domainSchema: DomainSchemaService[F])
    extends TypeDBQueryBuilder[F] {

  def build(
      queryDataRaw: DataForDBQuery,
      logic: DBQueryProperties,
      unlimitedQuery: Boolean,
      configLimit: Int,
      domain: Domain,
      countQuery: Boolean = false,
      countDistinctQuery: Boolean = false
  ): F[String] = {
    val queryData                  = addRelations(queryDataRaw)
    val headlines                  = queryData.entityList.map(_.schema.header)
    val attributesToIncludeToQuery =
      if (countQuery) Seq.empty[String] else logic.visualization :++ headlines

    val entityClauseF = queryData.entityList.foldLeftM("match ") { (query, entity) =>
      for {
        attrValues       <- domainSchema.sqlNamesMap(domain)
        addEntity         = s"$$${entity.entityName} isa ${entity.entityName}; "
        addAttributeNames =
          entity.attributes
            .filter(a => a.attributeValues.nonEmpty || attributesToIncludeToQuery.contains(a.attributeName))
            .distinctBy(_.attributeName)
            .map(a =>
              s"$$${entity.entityName} has ${attrValues.getOrElse(a.attributeName, a.attributeName)} $$${a.attributeName};"
            )
            .mkString(" ")
        attributesClauseF =
          if (entity.attributes.isEmpty) "".pure[F]
          else queryHelper.collectAggregateClause(entity.attributes, domain)

        res <- attributesClauseF.map(attributesClause => query + addEntity + addAttributeNames + attributesClause)
      } yield res
    }

    val relationClauseF = queryData.relationList.foldLeftM("") { (query, relation) =>
      val addRelation       =
        s"$$${relation.relationName} (${relation.entities.map(e => s"$$$e").mkString(", ")}) isa ${relation.relationName}; "
      val addAttributeNames = relation.attributes
        .collect {
          case a if a.attributeValues.nonEmpty || attributesToIncludeToQuery.contains(a.attributeName) =>
            s"$$${relation.relationName} has ${a.attributeName} $$${a.attributeName};"
        }
        .mkString(" ")
      val attributesClauseF =
        if (relation.attributes.isEmpty) "".pure[F]
        else queryHelper.collectAggregateClause(relation.attributes, domain)

      attributesClauseF.map(attributesClause => query + addRelation + addAttributeNames + attributesClause)
    }
    val getClause       = "get " + {
      queryData.entityList
        .map(_.entityName) ++ queryData.entityList
        .flatMap(_.attributes)
        .filter(a => attributesToIncludeToQuery.contains(a.attributeName))
        .map(_.attributeName) ++ queryData.relationList
        .map(_.relationName) ++ queryData.relationList
        .flatMap(_.attributes)
        .filter(a => attributesToIncludeToQuery.contains(a.attributeName))
        .map(_.attributeName)
    }.distinct.map(s => s"$$$s").mkString(", ") + ";"
    val offsetClause    =
      if (unlimitedQuery) ""
      else
        queryData.pagination
          .flatMap(_.page)
          .fold("")(page => s"offset ${page * queryData.pagination.flatMap(_.perPage).getOrElse(configLimit)};")
    val limitClause     =
      if (unlimitedQuery) ""
      else queryData.pagination.flatMap(_.perPage).fold(s"limit $configLimit;")(limit => s"limit $limit;")

    for {
      entityClause   <- entityClauseF
      relationClause <- relationClauseF
      resultQuery     = entityClause + relationClause + getClause + offsetClause + limitClause
    } yield resultQuery

  }

  private def addRelations(queryData: DataForDBQuery): DataForDBQuery = {
    val relations =
      relationsMap
        .filter { case (_, v) => v.subsetOf(queryData.entityList.map(_.entityName).toSet) }
        .map { case (k, v) =>
          RelationForDBQuery(relationName = k, entities = v.toList)
        }
        .toList
    queryData.copy(relationList = relations)
  }

}
