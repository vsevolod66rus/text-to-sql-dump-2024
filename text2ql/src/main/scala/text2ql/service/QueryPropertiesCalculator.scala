package text2ql.service

import cats.effect.{Resource, Sync}
import cats.implicits._
import text2ql.api._
import text2ql.error.ServerError.ServerErrorWithMessage
import text2ql.service.DomainSchemaService._

trait UserRequestTypeCalculator[F[_]] {
  def calculateDBQueryProperties(entities: List[ClarifiedNamedEntity], domain: Domain): F[DBQueryProperties]
}

object UserRequestTypeCalculator {

  def apply[F[+_]: Sync](domainSchema: DomainSchemaService[F]): Resource[F, UserRequestTypeCalculator[F]] =
    Resource.eval(Sync[F].delay(new UserRequestTypeCalculatorImpl[F](domainSchema)))
}

class UserRequestTypeCalculatorImpl[F[+_]: Sync](domainSchema: DomainSchemaService[F])
    extends UserRequestTypeCalculator[F] {

  def calculateDBQueryProperties(entities: List[ClarifiedNamedEntity], domain: Domain): F[DBQueryProperties] =
    for {
      targetOpt        <- entities.find(_.isTarget).pure[F]
      targetVertexName <- getTargetVertexName(targetOpt, domain)
      targetAttrName   <- getTargetAttrName(targetOpt, domain)
    } yield DBQueryProperties(
      targetAttr = targetAttrName,
      targetThing = targetVertexName,
      sortModelOpt = None,
      visualization = List.empty
    )

  private def getTargetVertexName(targetOpt: Option[ClarifiedNamedEntity], domain: Domain): F[String] = for {
    targetEntity        <- Sync[F].fromOption(targetOpt, ServerErrorWithMessage("no target entity from nlp"))
    targetEntityNameOpt <- targetEntity.tag match {
                             case E_TYPE => targetEntity.findFirstNamedValue.pure[F]
                             case A_TYPE =>
                               targetEntity.findFirstNamedValue.traverse(v =>
                                 domainSchema.getThingByAttribute(domain)(v)
                               )
                             case _      => domainSchema.getThingByAttribute(domain)(targetEntity.tag).map(_.some)
                           }
    targetEntityName    <-
      Sync[F].fromOption(targetEntityNameOpt, ServerErrorWithMessage("no selected value for target entity"))
  } yield targetEntityName

  private def getTargetAttrName(
      targetOpt: Option[ClarifiedNamedEntity],
      domain: Domain
  ): F[String] = for {
    targetEntity        <- Sync[F].fromOption(targetOpt, ServerErrorWithMessage("no target entity from nlp"))
    targetEntityNameOpt <- targetEntity.tag match {
                             case E_TYPE =>
                               targetEntity.findFirstNamedValue.traverse { v =>
                                 domainSchema.thingKeys(domain).map(_.getOrElse(v, v))
                               }
                             case _      => targetEntity.findFirstNamedValue.pure[F]
                           }
    targetAttrName      <-
      Sync[F].fromOption(targetEntityNameOpt, ServerErrorWithMessage("no selected value for target attr"))
  } yield targetAttrName

}
