package text2ql.service

import cats.effect.{Async, Resource}
import cats.implicits._
import fs2.{Stream, text}
import org.typelevel.log4cats.Logger
import text2ql.api._
import text2ql.configs.{DBDataConfig, TypeDBConfig}
import text2ql.domainschema.CheckDomainSchemaResponse
import text2ql.dao.typedb._
import text2ql.dao.postgres.{DomainRepo, QueryBuilder, QueryManager, ResponseBuilder}
import text2ql.service.DomainSchemaService._

import java.util.UUID

trait DomainSchemaChecker[F[_]] {
  def baseCheck(domain: Domain, content: Stream[F, Byte]): F[List[CheckDomainSchemaResponse]]

  def relationsCheck(
      domain: Domain,
      entities: List[String],
      onlySet: Boolean,
      content: Stream[F, Byte]
  ): F[List[CheckDomainSchemaResponse]]
}

object DomainSchemaChecker {

  def apply[F[+_]: Async: Logger](
      qm: QueryManager[F],
      conf: DBDataConfig
  ): Resource[F, DomainSchemaChecker[F]] = for {
    domainSchemaServ      <- DomainSchemaService[F]
    updater               <- QueryDataUpdater[F](domainSchemaServ)
    requestTypeCalculator <- UserRequestTypeCalculator[F](domainSchemaServ)
    queryDataCalc         <- QueryDataCalculator[F](updater, requestTypeCalculator)
    qb                    <- QueryBuilder[F](domainSchemaServ, conf)
    rb                    <- ResponseBuilder[F](domainSchemaServ)
    repo                  <- DomainRepo[F](qb, qm, rb)


  } yield new DomainSchemaCheckerImpl[F](queryDataCalc, repo, domainSchemaServ, qb)
}

class DomainSchemaCheckerImpl[F[+_]: Async](
    queryDataCalculator: QueryDataCalculator[F],
    askRepo: DomainRepo[F],
    domainSchema: DomainSchemaService[F],
    qb: QueryBuilder[F]
) extends DomainSchemaChecker[F] {

  def baseCheck(domain: Domain, content: Stream[F, Byte]): F[List[CheckDomainSchemaResponse]] = for {
    _             <- content.through(text.utf8.decode).compile.string.flatMap(domainSchema.update(domain, _))
    entityNames   <- domainSchema.vertices(domain).map(_.keySet.toList)
    samples        = buildClarifiedEntities(entityNames.map(List(_)))
    queryDataList <- samples.traverse(e => queryDataCalculator.prepareDataForQuery(e, baseUserReq, domain))
    res           <- askQueries(queryDataList)
  } yield res

  def relationsCheck(
      domain: Domain,
      entities: List[String],
      onlySet: Boolean,
      content: Stream[F, Byte]
  ): F[List[CheckDomainSchemaResponse]] = for {
    _                   <- content.through(text.utf8.decode).compile.string.flatMap(domainSchema.update(domain, _))
    entityNames         <- domainSchema.vertices(domain).map(_.keySet.toList).map { lst =>
                             if (entities.nonEmpty) lst.filter(entities.contains) else lst
                           }
    entitiesSeq          = if (onlySet) List(entityNames) else (1 to entityNames.size).flatMap(entityNames.combinations).toList
    samples              = buildClarifiedEntities(entitiesSeq)
    queryDataListEither <- samples.traverse { entities =>
                             queryDataCalculator.prepareDataForQuery(entities, baseUserReq, domain).attempt
                           }
    queryDataList        = queryDataListEither.collect { case Right(value) => value }
    res                 <- askQueries(queryDataList)
  } yield res

  private val baseUserReq = ChatMessageRequestModel(
    page = 0.some,
    perPage = 10.some,
    sort = BaseSortModel(None, None),
    chat = AddChatMessageRequestModel(message = "", requestId = UUID.randomUUID())
  )

  private def buildClarifiedEntities(entitiesList: List[List[String]]): List[List[ClarifiedNamedEntity]] =
    entitiesList.map { names =>
      names.map { e =>
        ClarifiedNamedEntity(
          tag = E_TYPE,
          originalValue = e,
          start = 0,
          namedValues = List(e),
          attributeSelected = None,
          isTarget = names.headOption.contains(e)
        )
      }
    }

  private def askQueries(queryDataList: List[DataForDBQuery]): F[List[CheckDomainSchemaResponse]] =
    queryDataList.traverse { queryData =>
      for {
        constantSqlChunk <- qb.buildConstantSqlChunk(queryData)
        buildQueryDTO    <- qb.buildGeneralSqlQuery(queryData, constantSqlChunk)
        res              <- askRepo
                              .generalQuery(queryData)
                              .attempt
                              .map(_.map(_.custom.flatMap(_.grid).get).leftMap(_.getMessage))
        entities          = queryData.entityList.map(_.entityName)
        relations         = queryData.relationList.map(_.relationName)
      } yield CheckDomainSchemaResponse(entities, relations, buildQueryDTO, res)
    }
}
