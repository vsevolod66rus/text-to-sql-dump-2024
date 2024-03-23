package text2ql.service

import cats.effect.{Async, Resource}
import cats.implicits._
import text2ql.api._

trait QueryDataCalculator[F[_]] {

  def prepareDataForQuery(
      clarifiedEntities: List[ClarifiedNamedEntity],
      userRequest: ChatMessageRequestModel,
      domain: Domain
  ): F[DataForDBQuery]
}

object QueryDataCalculator {

  def apply[F[+_]: Async](
      updater: QueryDataUpdater[F],
      requestTypeCalculator: UserRequestTypeCalculator[F]
  ): Resource[F, QueryDataCalculatorImpl[F]] =
    Resource.eval(Async[F].delay(new QueryDataCalculatorImpl(updater, requestTypeCalculator)))
}

class QueryDataCalculatorImpl[F[+_]: Async](
    updater: QueryDataUpdater[F],
    requestTypeCalculator: UserRequestTypeCalculator[F]
) extends QueryDataCalculator[F] {

  override def prepareDataForQuery(
      clarifiedEntities: List[ClarifiedNamedEntity],
      userRequest: ChatMessageRequestModel,
      domain: Domain
  ): F[DataForDBQuery] =
    for {
      reqProperties      <- requestTypeCalculator.calculateDBQueryProperties(clarifiedEntities, domain)
      requestId           = userRequest.chat.requestId
      initialDataForQuery = DataForDBQuery(
                              requestId = requestId,
                              entityList = List.empty[EntityForDBQuery],
                              relationList = List.empty[RelationForDBQuery],
                              properties = reqProperties,
                              pagination = userRequest.some,
                              domain = domain
                            )
      nonEmptySlots       = clarifiedEntities.map(_.tag)
      dataForQuery       <-
        nonEmptySlots.foldLeftM(initialDataForQuery) { (acc, el) =>
          updater.updateDataForDBQuery(acc, clarifiedEntities, domain, el)
        }
      withVisualization   = calculateVisualization(dataForQuery)
    } yield withVisualization

  private def calculateVisualization(
      queryData: DataForDBQuery,
      nAttributesLimit: Int = 50
  ): DataForDBQuery = {
    val visualization = {
      val visualization =
        List(
          queryData.entityList.flatMap(_.attributes),
          queryData.relationList.flatMap(_.attributes)
        ).flatten.map(_.attributeName)
      visualization.take(nAttributesLimit)
    }
    val updatedLogic = queryData.properties.copy(visualization = visualization)
    queryData.copy(properties = updatedLogic)
  }
}
