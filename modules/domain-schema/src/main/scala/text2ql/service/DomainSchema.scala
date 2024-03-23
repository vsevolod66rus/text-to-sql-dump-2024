package text2ql.service

import cats.effect._
import cats.implicits._
import io.circe.yaml.{parser => yamlParser}
import text2ql.api.Domain
import text2ql.domainschema.{DomainSchemaAttribute, DomainSchemaDTO, DomainSchemaEdge, DomainSchemaVertex}
import text2ql.error.ServerError.ServerErrorWithMessage
import text2ql.service.DomainSchema.DomainSchemaInternal

trait DomainSchema[F[_]] {
  def update(domainSchema: String): F[Unit]
  def schemaAttributesType: F[Map[String, String]]
  def vertices: F[Map[String, DomainSchemaVertex]]
  def attributesTitle: F[Map[String, String]]
  def sqlNames: F[Map[String, String]]
  def edges: F[List[DomainSchemaEdge]]
  def thingKeys: F[Map[String, String]]
  def thingKeysSQL: F[Map[String, String]]
  def thingAttributes: F[Map[String, Set[String]]]
}

object DomainSchema {

  def apply[F[_]: Async](domain: Domain): Resource[F, DomainSchema[F]] =
    Resource.eval {
      for {
        raw              <- "".pure[F] // from db
        maybeJson         = yamlParser.parse(raw).some
        maybeDomainSchema = maybeJson.flatMap(_.toOption.flatMap(_.as[DomainSchemaDTO].toOption))
        domainSchema      = maybeDomainSchema.map(new DomainSchemaInternal(_))
        result           <- Ref.of[F, Option[DomainSchemaInternal]](domainSchema).map(new DomainSchemaImpl(_, domain))
      } yield result
    }

  final class DomainSchemaInternal(domainSchemaDTO: DomainSchemaDTO) {

    lazy val vertices: Map[String, DomainSchemaVertex] = domainSchemaDTO.vertices.map(v => v.vertexName -> v).toMap

    lazy val attributes: Seq[DomainSchemaAttribute] = domainSchemaDTO.attributes

    lazy val attributesTypeMap: Map[String, String] = makeMapFromAttrs(a => a.attributeName -> a.attributeType)

    lazy val attributesTitleMap: Map[String, String] =
      makeMapFromAttrs(a => a.attributeName -> a.title) ++
        makeMapFromThings(th => th.vertexName -> th.title)

    lazy val sqlNames: Map[String, String]           = makeMapFromAttrs(a => a.attributeName -> a.attributeValue)

    lazy val edges: List[DomainSchemaEdge] = domainSchemaDTO.edges

    lazy val thingKeys: Map[String, String] = makeMapFromThings(th =>
      th.vertexName -> domainSchemaDTO.attributes
        .filter(_.vertexName == th.vertexName)
        .find(_.attributeValue == th.key)
        .map(_.attributeName)
        .getOrElse(th.key)
    )

    lazy val thingKeysSQL: Map[String, String] =
      domainSchemaDTO.vertices.map { vertex =>
        val value = sqlNames.getOrElse(vertex.key, vertex.key)
        vertex.vertexName -> value
      }.toMap

    lazy val thingAttributes: Map[String, Set[String]] =
      domainSchemaDTO.attributes.groupBy(_.vertexName).map { case (thing, attrs) =>
        thing -> attrs.map(_.attributeName).toSet
      }

    private def makeMapFromThings[K, V](f: DomainSchemaVertex => (K, V)): Map[K, V] =
      domainSchemaDTO.vertices.map(f).toMap

    private def makeMapFromAttrs[K, V](f: DomainSchemaAttribute => (K, V)): Map[K, V] =
      domainSchemaDTO.attributes.map(f).toMap

  }
}

final class DomainSchemaImpl[F[_]: Async](domainSchemaRef: Ref[F, Option[DomainSchemaInternal]], domain: Domain)
    extends DomainSchema[F] {

  private val domainSchema = for {
    maybeDomainSchema <- domainSchemaRef.get
    domainSchema      <- Async[F].fromOption(maybeDomainSchema, ServerErrorWithMessage(s" no schema for $domain"))
  } yield domainSchema

  override def update(domainSchemaYaml: String): F[Unit] = for {
    json            <- Async[F].delay(yamlParser.parse(domainSchemaYaml))
    newDomainSchema <- Async[F].delay(json.flatMap(_.as[DomainSchemaDTO]).fold(throw _, identity))
    _               <- domainSchemaRef.set(Some(new DomainSchemaInternal(newDomainSchema)))
  } yield ()

  override val vertices: F[Map[String, DomainSchemaVertex]] = domainSchema.map(_.vertices)
  override val schemaAttributesType: F[Map[String, String]] = domainSchema.map(_.attributesTypeMap)
  override val attributesTitle: F[Map[String, String]]      = domainSchema.map(_.attributesTitleMap)
  override val sqlNames: F[Map[String, String]]             = domainSchema.map(_.sqlNames)
  override val edges: F[List[DomainSchemaEdge]]             = domainSchema.map(_.edges)
  override val thingKeys: F[Map[String, String]]            = domainSchema.map(_.thingKeys)
  override val thingKeysSQL: F[Map[String, String]]         = domainSchema.map(_.thingKeysSQL)
  override val thingAttributes: F[Map[String, Set[String]]] = domainSchema.map(_.thingAttributes)
}
