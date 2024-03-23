import cats.Parallel
import cats.effect._
import cats.implicits._
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBClient
import exceptions.CustomExceptionHandler
import org.typelevel.log4cats.Logger
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import text2ql.configs.ApplicationConfig
import text2ql.controller.{DomainSchemaController, MigrationController}
import text2ql.dao.MigrationRepo
import text2ql.dao.postgres.{QueryBuilder, QueryManager}
import text2ql.dao.typedb.TypeDBTransactionManager
import text2ql.service.{DomainSchemaChecker, DomainSchemaService, MigrationService}
import text2ql.{TransactorProvider, WSApp, WebService}

object App extends WSApp[ApplicationConfig] {
  private val changelog   = "migrations/Changelog.xml".some
  private val classLoader = getClass.getClassLoader

  override def service[F[+_]: Async: Parallel: Logger]: Resource[F, WebService[F]] = {

//    def newTypeDBClient(url: String): Resource[F, TypeDBClient] =
//      Resource.fromAutoCloseable(Async[F].delay(TypeDB.coreClient(url)))

    // пусть полежит внутри F[] - чтобы не поднимать typeDB для старта приложения
    def newTypeDBClientF(url: String): Resource[F, F[TypeDBClient]] =
      Resource.make(Async[F].delay(Async[F].delay(TypeDB.coreClient(url))))(clientF => clientF.map(_.close()))

    for {
      conf                     <- Resource.eval(ConfigSource.default.loadF[F, ApplicationConfig]())
      xaStorage                <- TransactorProvider[F](changelog, classLoader, conf.database.storage)
      exceptionHandler         <- CustomExceptionHandler[F]
      domainSchema             <- DomainSchemaService[F]
      _                        <- QueryBuilder[F](domainSchema, conf.database.data)
      qm                       <- QueryManager[F](xaStorage)
      typeDBClientF            <- newTypeDBClientF(conf.typeDB.url)
      typeDBTransactionManager <- TypeDBTransactionManager[F](typeDBClientF, conf.typeDB)
      domainSchemaChecker      <- DomainSchemaChecker[F](qm, conf.database.data, typeDBTransactionManager, conf.typeDB)
      migrationRepo            <- MigrationRepo[F](xaStorage, typeDBTransactionManager, conf.database.data)
      migrationService         <- MigrationService[F](migrationRepo)
      schemaController         <- DomainSchemaController[F](domainSchema, domainSchemaChecker)
      migrationsController     <- MigrationController[F](migrationService)
    } yield WebService[F](
      Seq(schemaController, migrationsController),
      exceptionHandler
    )
  }
}
