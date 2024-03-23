package exceptions

import cats.effect.Sync
import cats.effect.kernel.Resource
import cats.implicits._
import text2ql.error.ServerError._
import sttp.model.StatusCode._
import sttp.monad.MonadError
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.server.model.ValuedEndpointOutput
import text2ql.EndpointError.{internalServerError, tooManyRequests}
import org.typelevel.log4cats.Logger

object CustomExceptionHandler {

  private type Output[F[_]] = F[Option[ValuedEndpointOutput[_]]]

  def apply[F[_]: Sync: Logger]: Resource[F, ExceptionHandler[F]] = {
    lazy val exceptionHandler = new ExceptionHandler[F] {
      override def apply(ctx: ExceptionContext)(implicit monad: MonadError[F]): Output[F] = for {
        _      <- Logger[F].error(ctx.e.toString)
        output <- handleException(ctx.e)
      } yield output
    }

    Resource.eval(Sync[F].delay(exceptionHandler))
  }

  private def handleException[F[_]: MonadError](e: Throwable): F[Option[ValuedEndpointOutput[_]]] = e match {
    case TypeDBConnectionsLimitExceeded => tooManyRequests("typeDBNoFreeConnections").outputWithCode(TooManyRequests)
    case e: ServerErrorWithMessage      => internalServerError(e.message).outputWithCode(TooManyRequests)
    case _                              => internalServerError("internalServerErrorMsg").outputWithCode(InternalServerError)
  }

}
