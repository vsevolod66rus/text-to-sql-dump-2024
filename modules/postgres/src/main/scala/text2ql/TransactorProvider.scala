package text2ql

import cats.effect.{Async, Resource}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import text2ql.configs.DBInstanceConfig

import java.sql.DriverManager

object TransactorProvider {
  private val DRIVER_NAME = "org.postgresql.Driver"

  def apply[F[_]: Async](
      changelogFileOpt: Option[String],
      classLoader: ClassLoader,
      config: DBInstanceConfig
  ): Resource[F, Transactor[F]] = for {
    connectEC  <- ExecutionContexts.fixedThreadPool(config.connectionThreads)
    transactor <- HikariTransactor.newHikariTransactor(
                    DRIVER_NAME,
                    config.url,
                    config.username,
                    config.password,
                    connectEC
                  )
    _          <- Resource.eval(Async[F].blocking(liquibaseUpdate(changelogFileOpt, classLoader, config)))
  } yield transactor

  private def liquibaseUpdate(
      changelogFileOpt: Option[String],
      classLoader: ClassLoader,
      config: DBInstanceConfig
  ): Unit = changelogFileOpt.fold(()) { changelogFile =>
    val liquibase = new Liquibase(
      changelogFile,
      new ClassLoaderResourceAccessor(classLoader),
      DatabaseFactory
        .getInstance()
        .findCorrectDatabaseImplementation(
          new JdbcConnection(
            DriverManager.getConnection(config.url, config.username, config.password)
          )
        )
    )
    liquibase.update("main")
    liquibase.close()
  }

}
