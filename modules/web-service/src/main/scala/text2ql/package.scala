import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.{PublicEndpoint, endpoint}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir._

package object text2ql {
  type WSEndpoint[F[_]] = ServerEndpoint[Fs2Streams[F] with WebSockets, F]

  val baseEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint.in("api" / "v1")
}
