package text2ql.dao.typedb

import cats.effect.std.Semaphore
import cats.effect._
import cats.implicits._
import fs2.Stream
import org.typelevel.log4cats.Logger
import retry.RetryDetails._
import retry._
import text2ql.api.{AskRepoResponse, DBQueryProperties, DataForDBQuery, Domain, GridPropertyValue}
import text2ql.configs.TypeDBConfig
import text2ql.error.ServerError.TypeDBConnectionsLimitExceeded

import scala.concurrent.duration._

trait TypeDBDomainRepo[F[_]] {

  def generalTypeDBQueryWithPermitAndRetry(queryData: DataForDBQuery): F[AskRepoResponse]

  def typeDbStreamQuery(
      queryData: DataForDBQuery,
      logic: DBQueryProperties,
      domain: Domain
  ): F[Stream[F, Map[String, GridPropertyValue]]]
}

object TypeDBDomainRepo {

  def apply[F[+_]: Async: Logger](
      queryManager: TypeDBQueryManager[F],
      transactionManager: TypeDBTransactionManager[F],
      conf: TypeDBConfig
  ): Resource[F, TypeDBDomainRepo[F]] =
    Resource.eval(
      Semaphore[F](conf.maxConcurrentTypeDB.longValue)
        .map(new TypeDBDomainRepoImpl(_, queryManager, transactionManager, conf))
    )
}

class TypeDBDomainRepoImpl[F[+_]: Async: Logger](
    semaphore: Semaphore[F],
    queryManager: TypeDBQueryManager[F],
    transactionManager: TypeDBTransactionManager[F],
    conf: TypeDBConfig
) extends TypeDBDomainRepo[F] {

  def generalTypeDBQueryWithPermitAndRetry(queryData: DataForDBQuery): F[AskRepoResponse] = retryOnSomeErrors(
    semaphore.tryAcquire.ifM(
      semaphore.count.flatMap(c => Logger[F].info(s"typeDB semaphore count = $c")) >>
        Async[F].guarantee(
          queryManager.generalQuery(queryData),
          semaphore.release
        ),
      Async[F].raiseError(TypeDBConnectionsLimitExceeded)
    )
  )

  def typeDbStreamQuery(
      queryData: DataForDBQuery,
      logic: DBQueryProperties,
      domain: Domain
  ): F[Stream[F, Map[String, GridPropertyValue]]] = Async[F].delay {
    for {
      readTransaction <- Stream.resource(transactionManager.read(domain))
      answer          <- Stream
                           .eval(
                             queryManager.streamQuery(
                               queryData,
                               logic,
                               readTransaction,
                               domain
                             )
                           )
                           .flatten
    } yield answer
  }

  private def retryOnSomeErrors[A](effect: F[A]): F[A] = retryingOnSomeErrors(
    policy = retryPolice(conf.limitRetries, conf.constantDelay),
    isWorthRetrying = checkRetryError,
    onError = logError
  )(effect)

  private def retryPolice(limitRetries: Int, constantDelay: Int): RetryPolicy[F] =
    RetryPolicies.limitRetries[F](limitRetries).join(RetryPolicies.constantDelay(constantDelay.seconds))

  private def checkRetryError(err: Throwable): F[Boolean] = Async[F].pure {
    err match {
      case TypeDBConnectionsLimitExceeded => true
      case _                              => false
    }
  }

  private def logError(err: Throwable, details: RetryDetails): F[Unit] =
    err match {
      case TypeDBConnectionsLimitExceeded =>
        details match {
          case WillDelayAndRetry(_, retriesSoFar: Int, _)              =>
            Logger[F].info(s"Ожидание свободного подключения к typedDB. Использовано $retriesSoFar попыток")
          case GivingUp(totalRetries: Int, totalDelay: FiniteDuration) =>
            Logger[F].info(
              s"Ожидание свободного подключения к typeDB - безуспешно после $totalRetries попыток, всего ожидания ${totalDelay.toString}."
            )
        }
      case _                              => Logger[F].error(s"Ошибка TypeDB: ${err.getMessage}")
    }

}
