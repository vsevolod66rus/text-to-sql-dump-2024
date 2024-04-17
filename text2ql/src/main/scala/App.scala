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

    for {
      conf                     <- Resource.eval(ConfigSource.default.loadF[F, ApplicationConfig]())
      xaStorage                <- TransactorProvider[F](changelog, classLoader, conf.database.storage)
      exceptionHandler         <- CustomExceptionHandler[F]
      domainSchema             <- DomainSchemaService[F]
      _                        <- QueryBuilder[F](domainSchema, conf.database.data)
      qm                       <- QueryManager[F](xaStorage)
      domainSchemaChecker      <- DomainSchemaChecker[F](qm, conf.database.data)
      migrationRepo            <- MigrationRepo[F](xaStorage, conf.database.data)
      migrationService         <- MigrationService[F](migrationRepo)
      schemaController         <- DomainSchemaController[F](domainSchema, domainSchemaChecker)
      migrationsController     <- MigrationController[F](migrationService)
    } yield WebService[F](
      Seq(schemaController, migrationsController),
      exceptionHandler
    )
  }
}
