package text2ql.dao.typedb

import cats.effect._
import cats.implicits._
import com.vaticle.typedb.client.api._
import text2ql.api.Domain
import text2ql.configs.TypeDBConfig

trait TypeDBTransactionManager[F[_]] {
  def read(domain: Domain): Resource[F, TypeDBTransaction]
  def write(domain: Domain): Resource[F, TypeDBTransaction]
}

object TypeDBTransactionManager {

  def apply[F[+_]: Sync](
      client: F[TypeDBClient], // пусть полежит внутри F[] - чтобы не поднимать typeDB для старта приложения
      conf: TypeDBConfig
  ): Resource[F, TypeDBTransactionManager[F]] =
    Resource.eval(Sync[F].delay(new TypeDBTransactionManagerImpl(client, conf)))

}

final class TypeDBTransactionManagerImpl[F[+_]: Sync](
    client: F[TypeDBClient],
    conf: TypeDBConfig
) extends TypeDBTransactionManager[F] {

  override def read(domain: Domain): Resource[F, TypeDBTransaction] =
    openTypeDBSession(domain).flatMap { session =>
      Resource.fromAutoCloseable(openTypeDBTransaction(session, TypeDBTransaction.Type.READ))
    }

  override def write(domain: Domain): Resource[F, TypeDBTransaction] =
    openTypeDBSession(domain).flatMap { session =>
      Resource.fromAutoCloseable(openTypeDBTransaction(session, TypeDBTransaction.Type.WRITE))
    }

//  private def openTypeDBSession(domain: Domain): Resource[F, TypeDBSession] =
//    Resource.fromAutoCloseable(getDbName(domain).map(client.session(_, TypeDBSession.Type.DATA)))

  private def openTypeDBSession(domain: Domain): Resource[F, TypeDBSession] =
    Resource.fromAutoCloseable(
      for {
        dbname  <- getDbName(domain)
        session <- client.map(_.session(dbname, TypeDBSession.Type.DATA))
      } yield session
    )

  private def openTypeDBTransaction(
      session: TypeDBSession,
      transactionType: TypeDBTransaction.Type
  ): F[TypeDBTransaction] =
    Sync[F].delay {
      session.transaction(
        transactionType,
        TypeDBOptions
          .core()
          .infer(conf.rules)
          .parallel(conf.parallel)
          .transactionTimeoutMillis(conf.transactionTimeoutMillis)
      )
    }

  private def getDbName(domain: Domain): F[String] = Sync[F].delay {
    domain match {
      case Domain.HR => conf.dbHR
    }
  }

}
