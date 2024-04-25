package text2ql.service

import cats.effect.{Resource, Sync}
import cats.implicits._
import text2ql.api._
import text2ql.error.ServerError.ServerErrorWithMessage
import text2ql.service.DomainSchemaService._

trait UserRequestTypeCalculator[F[_]] {
  def calculateDBQueryProperties(entities: List[EnrichedNamedEntity], domain: Domain): F[DBQueryProperties]
}

object UserRequestTypeCalculator {

  def apply[F[+_]: Sync](domainSchema: DomainSchemaService[F]): Resource[F, UserRequestTypeCalculator[F]] =
    Resource.eval(Sync[F].delay(new UserRequestTypeCalculatorImpl[F](domainSchema)))
}

class UserRequestTypeCalculatorImpl[F[+_]: Sync](domainSchema: DomainSchemaService[F])
    extends UserRequestTypeCalculator[F] {

  def calculateDBQueryProperties(entities: List[EnrichedNamedEntity], domain: Domain): F[DBQueryProperties] =
    for {
      targetOpt        <- entities.find(_.isTarget).pure[F]
      targetVertexName <- getTargetVertexName(targetOpt, domain)
      targetcolumnName <- getTargetcolumnName(targetOpt, domain)
    } yield DBQueryProperties(
      targetAttr = targetcolumnName,
      targetThing = targetVertexName,
      sort = None,
      visualization = List.empty,
      page = 0.some,
      perPage = 10.some
    )

  private def getTargetVertexName(targetOpt: Option[EnrichedNamedEntity], domain: Domain): F[String] = for {
    targetEntity        <- Sync[F].fromOption(targetOpt, ServerErrorWithMessage("no target entity from nlp"))
    targetEntityNameOpt <- targetEntity.tag match {
                             case TABLE  => targetEntity.value.some.pure[F]
                             case COLUMN =>
                               targetEntity.value.some.traverse(v => domainSchema.getTableByColumn(domain)(v))
                             case _      => domainSchema.getTableByColumn(domain)(targetEntity.tag).map(_.some)
                           }
    targetEntityName    <-
      Sync[F].fromOption(targetEntityNameOpt, ServerErrorWithMessage("no selected value for target entity"))
  } yield targetEntityName

  private def getTargetcolumnName(
      targetOpt: Option[EnrichedNamedEntity],
      domain: Domain
  ): F[String] = for {
    targetEntity     <- Sync[F].fromOption(targetOpt, ServerErrorWithMessage("no target entity from nlp"))
    targetcolumnName <- targetEntity.tag match {
                          case TABLE =>
                            domainSchema.tableKeys(domain).map(_.getOrElse(targetEntity.value, targetEntity.value))
                          case _     => targetEntity.value.pure[F]
                        }
  } yield targetcolumnName

}
