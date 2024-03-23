package text2ql.dao.typedb

import cats.effect.kernel._
import cats.implicits._
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import fs2.Stream
import org.typelevel.log4cats.Logger
import text2ql.api._
import text2ql.configs.TypeDBConfig
import text2ql.error.ServerError.{ServerErrorWithMessage, TypeDBQueryException}

import scala.jdk.CollectionConverters._
import scala.util.Try

trait TypeDBQueryManager[F[_]] {

  def generalQuery(queryData: DataForDBQuery): F[AskRepoResponse]

  def streamQuery(
      queryData: DataForDBQuery,
      logic: DBQueryProperties,
      readTransaction: TypeDBTransaction,
      domain: Domain
  ): F[Stream[F, Map[String, GridPropertyValue]]]
}

object TypeDBQueryManager {

  def apply[F[_]: Sync: Logger](
      transactionManager: TypeDBTransactionManager[F],
      queryBuilder: TypeDBQueryBuilder[F],
      queryHelper: TypeDBResponseBuilder[F],
      conf: TypeDBConfig
  ): Resource[F, TypeDBQueryManager[F]] =
    Resource.eval(
      Sync[F].delay(new TypeDBQueryManagerImpl(transactionManager, queryBuilder, queryHelper, conf))
    )

}

final class TypeDBQueryManagerImpl[F[_]: Sync: Logger](
    transactionManager: TypeDBTransactionManager[F],
    queryBuilder: TypeDBQueryBuilder[F],
    queryHelper: TypeDBResponseBuilder[F],
    conf: TypeDBConfig
) extends TypeDBQueryManager[F] {

  override def streamQuery(
      queryData: DataForDBQuery,
      logic: DBQueryProperties,
      readTransaction: TypeDBTransaction,
      domain: Domain
  ): F[Stream[F, Map[String, GridPropertyValue]]] = for {
    query      <- queryBuilder.build(queryData, logic, unlimitedQuery = true, conf.limit, domain)
    getQuery    = TypeQL.parseQuery[TypeQLMatch](query)
    _          <- Logger[F].info(s"try typeDB stream Query: $query")
    iterator    = readTransaction.query.`match`(getQuery).iterator()
    raw         = iterator.asScala.toSeq
    properties <- queryHelper.makeGridProperties(queryData, raw)
    res         = Stream.fromIterator(iterator = iterator.asScala, chunkSize = 1).map { cm =>
                    Map("id" -> GridPropertyValueString(java.util.UUID.randomUUID().toString)) ++
                      properties.groupMapReduce(_.key) { prop =>
                        GridPropertyValueString(queryHelper.getCMAttributeStringValue(cm, prop.key))
                      }((_, value) => value)
                  }
  } yield res

  override def generalQuery(queryData: DataForDBQuery): F[AskRepoResponse] = transactionManager
    .read(queryData.domain)
    .use { readTransaction =>
      val logic  = queryData.properties
      val domain = queryData.domain
      val countF = getCount(queryData, logic, readTransaction, domain)

      for {
        count <- countF
        query <- queryBuilder.build(queryData, logic, unlimitedQuery = false, conf.limit, domain)

        _           <- Logger[F].info(s"try TQL query: $query")
        queryResult <- typeQLMatchQuery(readTransaction, query).map(_.iterator().asScala.toSeq)

        page    = queryData.pagination.flatMap(_.page).getOrElse(0)
        perPage = queryData.pagination.flatMap(_.perPage).getOrElse(conf.limit)
        offset  = page * perPage
        limit   = perPage
        result <- if (queryResult.nonEmpty)
                    queryHelper
                      .makeGrid(
                        queryData,
                        queryResult,
                        count.countRecords,
                        domain,
                        offset,
                        limit
                      )
                      .map { grid =>
                        AskRepoResponse(
                          custom = AskResponsePayload(grid, pagination = queryData.pagination).some,
                          count = count,
                          query = query.some
                        )
                      }
                  else AskRepoResponse(text = "По Вашему запросу данных не найдено.".some, count = count).pure[F]
      } yield result
    }

  private def getCount(
      queryData: DataForDBQuery,
      logic: DBQueryProperties,
      readTransaction: TypeDBTransaction,
      domain: Domain
  ): F[CountQueryDTO] = {

    def getQuery(desc: Boolean = false) = queryBuilder.build(
      queryData,
      logic,
      unlimitedQuery = true,
      conf.limit,
      domain,
      countQuery = true,
      countDistinctQuery = desc
    )

    for {
      queryCountGeneral <- getQuery().map(_ + "count;")
      countGeneral      <- getCountQueryResult(queryCountGeneral, readTransaction)
      queryCountTarget  <- getQuery(desc = true).map(_ + "count;")
      countTarget       <- getCountQueryResult(queryCountTarget, readTransaction)
    } yield CountQueryDTO(
      countRecords = countGeneral.countRecords,
      countTarget = countTarget.countRecords.some,
      query = countGeneral.query
    )
  }

  private def getCountQueryResult(
      query: String,
      readTransaction: TypeDBTransaction
  ): F[CountQueryDTO] =
    for {
      typeQLQuery <- Sync[F].delay(TypeQL.parseQuery[TypeQLMatch.Aggregate](query))
      countOpt    <- Sync[F].delay(Try(readTransaction.query.`match`(typeQLQuery).get().asLong()).toOption)
      count       <- Sync[F].fromOption(countOpt, ServerErrorWithMessage("no result: count query error"))
      countDTO     = CountQueryDTO(
                       countRecords = count,
                       countTarget = None,
                       query = query
                     )
    } yield countDTO

  private def typeQLMatchQuery(
      readTransaction: TypeDBTransaction,
      tqlQuery: String
  ): F[java.util.stream.Stream[ConceptMap]] =
    Sync[F]
      .blocking(readTransaction.query().`match`(TypeQL.parseQuery[TypeQLMatch](tqlQuery)))
      .adaptError(TypeDBQueryException(_))

}
