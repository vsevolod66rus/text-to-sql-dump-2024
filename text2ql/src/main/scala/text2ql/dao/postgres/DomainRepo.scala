package text2ql.dao.postgres

import cats.effect.{Async, Resource}
import cats.implicits._
import fs2.Stream
import org.typelevel.log4cats.Logger
import text2ql.api.{AskRepoResponse, DataForDBQuery, GridPropertyValue}

trait DomainRepo[F[_]] {
  def generalQuery(queryData: DataForDBQuery): F[AskRepoResponse]
  def streamQuery(queryData: DataForDBQuery): F[Stream[F, Map[String, GridPropertyValue]]]
}

object DomainRepo {

  def apply[F[_]: Async: Logger](
      qb: QueryBuilder[F],
      qm: QueryManager[F],
      responseBuilder: ResponseBuilder[F]
  ): Resource[F, DomainRepo[F]] =
    Resource.eval(Async[F].delay(new DomainRepoImpl[F](qb, qm, responseBuilder)))
}

class DomainRepoImpl[F[_]: Async: Logger](
    qb: QueryBuilder[F],
    qm: QueryManager[F],
    responseBuilder: ResponseBuilder[F]
) extends DomainRepo[F] {

  override def generalQuery(queryData: DataForDBQuery): F[AskRepoResponse] = for {
    constantSqlChunk <- qb.buildConstantSqlChunk(queryData)
    buildQueryDTO    <- qb.buildGeneralSqlQuery(queryData, constantSqlChunk)
    countDTO         <- qm.getCount(buildQueryDTO)
    _                <- Logger[F].info(s"try SQL: ${buildQueryDTO.generalQuery}")
    generalQueryDTO  <- qm.getGeneralQueryDTO(buildQueryDTO.generalQuery)
    res              <- responseBuilder.buildResponse(queryData, buildQueryDTO, generalQueryDTO, countDTO)
  } yield res

  override def streamQuery(queryData: DataForDBQuery): F[Stream[F, Map[String, GridPropertyValue]]] = for {
    constantSqlChunk <- qb.buildConstantSqlChunk(queryData)
    query            <- qb.buildGeneralSqlQuery(queryData, constantSqlChunk).map(_.generalQuery)
    props            <- responseBuilder.makeGridProperties(queryData)
    res               = qm.getGenericPgRowStream(query)
                          .evalMap { pgRow =>
                            val headers = pgRow.keys.toVector
                            val row     = pgRow.values.toVector
                            responseBuilder.toItem(headers, props, queryData.domain)(row)
                          }
  } yield res

}
