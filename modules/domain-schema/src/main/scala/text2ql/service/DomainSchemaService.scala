package text2ql.service

import cats.implicits._
import cats.effect._
import fs2.text
import text2ql.api.Domain
import text2ql.domainschema.{DomainSchemaDTO, DomainSchemaRelation, DomainSchemaTable}
import text2ql.error.ServerError.ServerErrorWithMessage
import io.circe.yaml.{parser => yamlParser}

trait DomainSchemaService[F[_]] {
  def uploadActive(domain: Domain, contentStream: fs2.Stream[F, Byte]): F[Unit]
  def update(domain: Domain, domainSchemaContent: String): F[Unit]
  def toSchema: PartialFunction[Domain, DomainSchema[F]]
  def vertices(domain: Domain): F[Map[String, DomainSchemaTable]]
  def schemaAttributesType(domain: Domain): F[Map[String, String]]
  def attributesTitle(domain: Domain): F[Map[String, String]]
  def sqlNames(domain: Domain, key: String): F[String]
  def sqlNamesMap(domain: Domain): F[Map[String, String]]
  def relations(domain: Domain): F[List[DomainSchemaRelation]]
  def thingKeys(domain: Domain): F[Map[String, String]]
  def thingTitle(originalName: String, domain: Domain): F[String]
  def thingKeysSQL(domain: Domain): F[Map[String, String]]
  def getColumnsByTable(domain: Domain)(thingName: String): F[List[String]]
  def getTableByColumn(domain: Domain)(attrName: String): F[String]
}

object DomainSchemaService {

  def apply[F[_]: Async]: Resource[F, DomainSchemaService[F]] = for {
    domainSchemaHR <- DomainSchema[F](Domain.HR)
  } yield new DomainSchemaServiceImpl(domainSchemaHR)

  val A_TYPE = "attribute_type"
  val E_TYPE = "entity_type"
  val CO     = "comparison_operator"

}

class DomainSchemaServiceImpl[F[_]: Async](
    domainSchemaHR: DomainSchema[F]
) extends DomainSchemaService[F] {

  def toSchema: PartialFunction[Domain, DomainSchema[F]] = { case Domain.HR =>
    domainSchemaHR
  }

  def uploadActive(domain: Domain, contentStream: fs2.Stream[F, Byte]): F[Unit] = for {
    content      <- contentStream.through(text.utf8.decode).compile.string
    domainSchema <- createAndActive(domain, content)
  } yield domainSchema

  private def createAndActive(domain: Domain, content: String): F[Unit] = for {
    _ <- Sync[F]
           .fromEither(yamlParser.parse(content).flatMap(_.as[DomainSchemaDTO]))
           .adaptError(e => ServerErrorWithMessage(e.getMessage))
    _ <- update(domain, content)
  } yield ()

  def update(domain: Domain, domainSchemaContent: String): F[Unit] = toSchema(domain).update(domainSchemaContent)

  def vertices(domain: Domain): F[Map[String, DomainSchemaTable]] = toSchema(domain).tables

  def schemaAttributesType(domain: Domain): F[Map[String, String]] = toSchema(domain).types

  def attributesTitle(domain: Domain): F[Map[String, String]] = toSchema(domain).titles

  def sqlNames(domain: Domain, key: String): F[String] =
    toSchema(domain).sqlNames.map(_.getOrElse(key, key))

  def sqlNamesMap(domain: Domain): F[Map[String, String]] =
    toSchema(domain).sqlNames

  def relations(domain: Domain): F[List[DomainSchemaRelation]] = toSchema(domain).relations

  def thingKeys(domain: Domain): F[Map[String, String]] = toSchema(domain).thingKeys

  def thingTitle(originalName: String, domain: Domain): F[String] =
    attributesTitle(domain).map(_.getOrElse(originalName, originalName))

  def thingKeysSQL(domain: Domain): F[Map[String, String]] = toSchema(domain).thingKeysSQL

  def getColumnsByTable(domain: Domain)(thingName: String): F[List[String]] =
    toSchema(domain).thingAttributes.map(_.getOrElse(thingName, Set.empty).toList)

  def getTableByColumn(domain: Domain)(attrName: String): F[String] =
    toSchema(domain).thingAttributes.map(
      _.find { case (_, v) => v.contains(attrName) }.map(_._1).getOrElse(s"Thing for $attrName not found")
    )

  private def thingAttributes(domain: Domain): F[Map[String, Set[String]]] = toSchema(domain).thingAttributes
}
