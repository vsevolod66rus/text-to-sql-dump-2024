package text2ql.controller

import cats.effect.{Async, Resource}
import cats.implicits._
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.interceptor.decodefailure.DefaultDecodeFailureHandler.OnDecodeFailure.RichEndpointTransput
import text2ql.api.Domain
import text2ql.domainschema.CheckDomainSchemaResponse
import text2ql.service.{DomainSchemaChecker, DomainSchemaService}
import text2ql.{Controller, EndpointError, WSEndpoint, baseEndpoint}

import java.nio.charset.StandardCharsets

object DomainSchemaController {

  def apply[F[_]: Async](
      domainSchemaService: DomainSchemaService[F],
      domainSchemaChecker: DomainSchemaChecker[F]
  ): Resource[F, DomainSchemaController[F]] = Resource.eval(
    Async[F].delay(
      new DomainSchemaController[F](domainSchemaService, domainSchemaChecker)
    )
  )
}

class DomainSchemaController[F[_]: Async](
    domainSchemaService: DomainSchemaService[F],
    domainSchemaChecker: DomainSchemaChecker[F]
) extends Controller[F] {

  private val adminBaseEndpoint = baseEndpoint.errorOut(jsonBody[EndpointError]).tag("Domain schema")

  override val name = "Domain schema"

  override def endpoints: Seq[WSEndpoint[F]] = Seq(
    adminBaseEndpoint.post
      .in("domainSchema")
      .in("upload")
      .in(path[Domain]("domain").onDecodeFailureNextEndpoint)
      .in(streamTextBody(Fs2Streams[F])(CodecFormat.OctetStream(), StandardCharsets.UTF_8.some))
      .out(stringBody)
      .serverLogicRecoverErrors { case (domain, body) =>
        domainSchemaService.uploadActive(domain, body).map(_ => "updated")
      },
    adminBaseEndpoint.post
      .in("domainSchema")
      .in("baseCheck")
      .in(path[Domain]("domain").onDecodeFailureNextEndpoint)
      .in(streamTextBody(Fs2Streams[F])(CodecFormat.OctetStream(), StandardCharsets.UTF_8.some))
      .out(jsonBody[List[CheckDomainSchemaResponse]])
      .serverLogicRecoverErrors { case (domain, body) =>
        domainSchemaChecker.baseCheck(domain, body)
      },
    adminBaseEndpoint.post
      .in("domainSchema")
      .in("relationsCheck")
      .in(path[Domain]("domain").onDecodeFailureNextEndpoint)
      .in(query[List[String]]("entities").description("leave blank for all domain entities"))
      .in(query[Boolean]("onlySet").description("don't test power set"))
      .in(streamTextBody(Fs2Streams[F])(CodecFormat.OctetStream(), StandardCharsets.UTF_8.some))
      .out(jsonBody[List[CheckDomainSchemaResponse]])
      .serverLogicRecoverErrors { case (domain, entities, onlySet, body) =>
        domainSchemaChecker.relationsCheck(domain, entities, onlySet, body)
      },
    adminBaseEndpoint.post
      .in("typeDB")
      .in("domainSchema")
      .in("baseCheck")
      .in(path[Domain]("domain").onDecodeFailureNextEndpoint)
      .in(streamTextBody(Fs2Streams[F])(CodecFormat.OctetStream(), StandardCharsets.UTF_8.some))
      .out(jsonBody[List[CheckDomainSchemaResponse]])
      .serverLogicRecoverErrors { case (domain, body) =>
        domainSchemaChecker.baseCheckTypeDB(domain, body)
      },
    adminBaseEndpoint.post
      .in("typeDB")
      .in("domainSchema")
      .in("relationsCheck")
      .in(path[Domain]("domain").onDecodeFailureNextEndpoint)
      .in(query[List[String]]("entities").description("leave blank for all domain entities"))
      .in(query[Boolean]("onlySet").description("don't test power set"))
      .in(streamTextBody(Fs2Streams[F])(CodecFormat.OctetStream(), StandardCharsets.UTF_8.some))
      .out(jsonBody[List[CheckDomainSchemaResponse]])
      .serverLogicRecoverErrors { case (domain, entities, onlySet, body) =>
        domainSchemaChecker.relationsCheckTypeDB(domain, entities, onlySet, body)
      }
  )
}
