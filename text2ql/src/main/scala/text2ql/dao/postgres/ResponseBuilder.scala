package text2ql.dao.postgres

import cats.effect.{Async, Resource}
import cats.implicits._
import text2ql.api._
import text2ql.service.DomainSchemaService

trait ResponseBuilder[F[_]] {

  def buildResponse(
      queryData: DataForDBQuery,
      buildQueryDTO: BuildQueryDTO,
      generalQueryDTO: GeneralQueryDTO,
      countQueryDTO: CountQueryDTO
  ): F[AskRepoResponse]

  def makeGridProperties(queryData: DataForDBQuery): F[List[GridPropertyItemModel]]

  def toItem(headers: Vector[String], properties: List[GridPropertyItemModel], domain: Domain)(
      row: Vector[String]
  ): F[Map[String, GridPropertyValue]]
}

object ResponseBuilder {

  def apply[F[_]: Async](
      domainSchema: DomainSchemaService[F]
  ): Resource[F, ResponseBuilder[F]] =
    Resource.eval(Async[F].delay(new ResponseBuilderImpl[F](domainSchema)))
}

class ResponseBuilderImpl[F[_]: Async](
    domainSchema: DomainSchemaService[F]
) extends ResponseBuilder[F] {

  override def buildResponse(
      queryData: DataForDBQuery,
      buildQueryDTO: BuildQueryDTO,
      generalQueryDTO: GeneralQueryDTO,
      countQueryDTO: CountQueryDTO
  ): F[AskRepoResponse] =
    for {
      properties <- makeGridProperties(queryData)
      items      <- generalQueryDTO.data.traverse(toItem(generalQueryDTO.headers, properties, queryData.domain))
      data        = GridWithDataRenderTypeResponseModel(
                      properties = properties,
                      items = items,
                      total = countQueryDTO.countRecords
                    )
      res         = toAskResponse(countQueryDTO, buildQueryDTO.generalQuery)(data)
    } yield res

  override def makeGridProperties(
      queryData: DataForDBQuery
  ): F[List[GridPropertyItemModel]] =
    buildTableProperties(queryData.properties, queryData.domain)

  override def toItem(headers: Vector[String], properties: List[GridPropertyItemModel], domain: Domain)(
      row: Vector[String]
  ): F[Map[String, GridPropertyValue]]         =
    properties
      .traverse(prop => getColumnValue(headers, prop.key, domain)(row).map(v => prop.key -> v))
      .map(_.groupMapReduce(_._1)(_._2)((_, v) => v))
      .map(m => Map("id" -> GridPropertyValueString(java.util.UUID.randomUUID().toString)) ++ m)

  private def getColumnValue(
      headers: Vector[String],
      column: String,
      domain: Domain
  )(row: Vector[String]): F[GridPropertyValue] = domainSchema.types(domain).map { attrs =>
    val stringValue = row(headers.indexOf(column))
    val attrType    = attrs.getOrElse(column, "string")
    GridPropertyValue.fromValueAndType(stringValue, attrType)
  }

  private def toAskResponse(
      count: CountQueryDTO,
      query: String
  )(grid: GridWithDataRenderTypeResponseModel): AskRepoResponse =
    if (count.countRecords == 0)
      AskRepoResponse(text = "По Вашему запросу данных не найдено.".some, count = count, query = query.some)
    else
      AskRepoResponse(
        custom = AskResponsePayload(grid).some,
        count = count,
        query = query.some
      )

  private def buildTableProperties(
      logic: DBQueryProperties,
      domain: Domain
  ): F[List[GridPropertyItemModel]] = logic.visualization
    .traverse { column =>
      for {
        attrType <- domainSchema.types(domain).map(_.getOrElse(column, "string"))
        title    <- domainSchema.titles(domain).map(_.getOrElse(column, column))
      } yield GridPropertyItemModel(
        key = column,
        title = title,
        dataType = GridPropertyDataType.fromType(attrType)
      )
    }

}
