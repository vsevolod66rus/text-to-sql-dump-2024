package text2ql

import cats.effect.Async
import cats.implicits._
import org.http4s._
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2
import sttp.apispec.openapi.circe.yaml.RichOpenAPI
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.interceptor.exception.ExceptionHandler
import sttp.tapir.swagger.SwaggerUI

case class WebService[F[_]: Async](
    controllers: Seq[Controller[F]],
    exceptionHandler: ExceptionHandler[F]
) {

  def getSocketBuilderToApp(wbs: WebSocketBuilder2[F]): HttpRoutes[F] = {

    val endpointsCommon = getServiceEndpoints ++ controllers.flatMap(_.endpoints)

    val serverOptions = Http4sServerOptions.customiseInterceptors
      .exceptionHandler(exceptionHandler)
      .options

    def allRoutes(wbs: WebSocketBuilder2[F]) =
      Http4sServerInterpreter(serverOptions)
        .toWebSocketRoutes(serverEndpoints = endpointsCommon)(wbs) <+>
        docsRoutes(endpointsCommon)
    Router[F]("/" -> allRoutes(wbs))
  }

  private def docsRoutes(endpoints: Seq[WSEndpoint[F]]): HttpRoutes[F] = {
    val docs = OpenAPIDocsInterpreter().serverEndpointsToOpenAPI(
      ses = endpoints,
      title = "text2ql-examples",
      version = "0.1.0"
    )
    Http4sServerInterpreter().toRoutes(SwaggerUI[F](docs.toYaml))
  }

  private def getServiceEndpoints: List[ServerEndpoint[Any, F]] = {
    val infoEndpoint = baseEndpoint
      .in("info")
      .out(stringBody)
      .serverLogicSuccess(_ => "info".pure[F])

    List(infoEndpoint).map(_.tag("Service"))
  }
}
