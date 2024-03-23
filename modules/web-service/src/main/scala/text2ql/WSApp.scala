package text2ql

import cats.Parallel
import cats.effect.kernel.Outcome
import cats.effect.{Async, ExitCode, IO, IOApp, Resource, Sync}
import cats.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.ServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.module.catseffect.syntax._
import pureconfig.ConfigSource
import text2ql.configs.ServerConfig

import scala.concurrent.duration._
import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

abstract class WSApp[C] extends IOApp {

  protected def service[F[+_]: Async: Parallel: Logger]: Resource[F, WebService[F]]

  override def run(args: List[String]): IO[ExitCode] = runProgram[IO]

  private def runProgram[F[+_]: Async: Parallel]: F[ExitCode] =
    for {
      implicit0(logger: Logger[F]) <- Slf4jLogger.create[F]
      result                       <- Sync[F].guaranteeCase(runServer[F]) {
                                        case Outcome.Succeeded(_) => logger.info("Server completed")
                                        case Outcome.Errored(e)   => logger.error(s"Server error: $e")
                                        case Outcome.Canceled()   => logger.warn("Server canceled")
                                      }
    } yield result

  private def runServer[F[+_]: Async: Parallel: Logger]: F[ExitCode] =
    server[F].use(s => Logger[F].info("Server started") >> s.serve.compile.lastOrError)

  private def server[F[+_]: Async: Parallel: Logger]: Resource[F, ServerBuilder[F]] =
    for {
      serverConfig <- Resource.eval(ConfigSource.default.at("server").loadF[F, ServerConfig]())
      ws           <- service[F]
      ec           <- createExecutionContext[F]
      server        = BlazeServerBuilder.apply
                        .withExecutionContext(ec)
                        .withoutBanner
                        .bindHttp(serverConfig.port, serverConfig.host)
                        .withHttpWebSocketApp(ws.getSocketBuilderToApp(_).orNotFound)
                        .withMaxRequestLineLength(Int.MaxValue)
                        .withResponseHeaderTimeout(4.minutes)
                        .withIdleTimeout(5.minutes)
    } yield server

  private def createExecutionContext[F[_]: Async]: Resource[F, ExecutionContextExecutor] = Resource
    .make(Async[F].blocking(Executors.newCachedThreadPool()))(ec => Async[F].blocking(ec.shutdown()))
    .map(ExecutionContext.fromExecutor)
}
