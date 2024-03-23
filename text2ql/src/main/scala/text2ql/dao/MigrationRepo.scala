package text2ql.dao

import cats.effect.Async
import cats.effect.kernel._
import cats.implicits._
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLInsert
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.Transactor
import org.typelevel.log4cats.Logger
import text2ql.migration._
import fs2.{Chunk, Stream}
import text2ql.api.Domain
import text2ql.configs.DBDataConfig
import text2ql.dao.typedb.TypeDBTransactionManager

import java.util.UUID
import java.time.{Instant, LocalDate, ZoneId}
import scala.util.Random

trait MigrationRepo[F[_]] {
  def generateEmployees(n: Int): F[Unit]
  def insertHrToTypeDB(): F[Unit]
}

object MigrationRepo {

  def apply[F[_]: Async: Logger](
      xaStorage: Transactor[F],
      transactionManager: TypeDBTransactionManager[F],
      conf: DBDataConfig
  ): Resource[F, MigrationRepo[F]] =
    Resource.eval(Sync[F].delay(new MigrationRepoImpl(xaStorage, transactionManager, conf)))
}

class MigrationRepoImpl[F[_]: Async: Logger](
    xaStorage: Transactor[F],
    transactionManager: TypeDBTransactionManager[F],
    conf: DBDataConfig
) extends MigrationRepo[F] {

  private val hrSchemaName = conf.pgSchemas.getOrElse(Domain.HR, Domain.HR.entryName)

  override def insertHrToTypeDB(): F[Unit] = for {
    _ <- insertRegionsTypeDB()
    _ <- insertCitiesTypeDB()
    _ <- insertLocationsTypeDB()
    _ <- insertDepartmentsTypeDB()
    _ <- insertEmployeesTypeDB()
    _ <- insertJobsTypeDB()
    _ <- insertJobFunctionsTypeDB()
    _ <- insertRelations()
  } yield ()

  private def insertRegionsTypeDB() = getHrRegionStream
    .chunkN(1000)
    .parEvalMap(1) { chunk =>
      transactionManager.write(Domain.HR).use { transaction =>
        for {
          _ <- Async[F]
                 .delay {
                   chunk
                     .map { r =>
                       val insertQueryStr =
                         s"""insert $$region isa region, has id "${r.id}", has code "${r.code}", has name "${r.name}";"""
                       val insertQuery    = TypeQL.parseQuery[TypeQLInsert](insertQueryStr)
                       transaction.query().insert(insertQuery)
                     }
                 }
          _  = transaction.commit()
          _  = transaction.close()
          _ <- Logger[F].info(s"inserted ${chunk.size} regions")
        } yield ()
      }
    }
    .compile
    .drain

  private def insertCitiesTypeDB() = getHrCitiesStream
    .chunkN(1000)
    .parEvalMap(1) { chunk =>
      transactionManager.write(Domain.HR).use { transaction =>
        for {
          _ <- Async[F]
                 .delay {
                   chunk
                     .map { c =>
                       val insertQueryStr =
                         s"""insert $$city isa city, has id "${c.id}", has region_id "${c.regionId}", has code "${c.code}", has name "${c.name}";"""
                       val insertQuery    = TypeQL.parseQuery[TypeQLInsert](insertQueryStr)
                       transaction.query().insert(insertQuery)
                     }
                 }
          _  = transaction.commit()
          _  = transaction.close()
          _ <- Logger[F].info(s"inserted ${chunk.size} cities")
        } yield ()
      }
    }
    .compile
    .drain

  private def insertLocationsTypeDB() = getHrLocationsStream
    .chunkN(1000)
    .parEvalMap(1) { chunk =>
      transactionManager.write(Domain.HR).use { transaction =>
        for {
          _ <- Async[F]
                 .delay {
                   chunk
                     .map { l =>
                       val insertQueryStr =
                         s"""insert $$location isa location, has id "${l.id}", has city_id "${l.cityId}", has code "${l.code}", has name "${l.name}";"""
                       val insertQuery    = TypeQL.parseQuery[TypeQLInsert](insertQueryStr)
                       transaction.query().insert(insertQuery)
                     }
                 }
          _  = transaction.commit()
          _  = transaction.close()
          _ <- Logger[F].info(s"inserted ${chunk.size} locations")
        } yield ()
      }
    }
    .compile
    .drain

  private def insertDepartmentsTypeDB() = getHrDepartmentsStream
    .chunkN(1000)
    .parEvalMap(1) { chunk =>
      transactionManager.write(Domain.HR).use { transaction =>
        for {
          _ <- Async[F]
                 .delay {
                   chunk
                     .map { d =>
                       val insertQueryStr =
                         s"""insert $$department isa department, has id "${d.id}", has location_id "${d.locationId}",
                            |has code "${d.code}", has name "${d.name}", has path "${d.path}";""".stripMargin
                       val insertQuery    = TypeQL.parseQuery[TypeQLInsert](insertQueryStr)
                       transaction.query().insert(insertQuery)
                     }
                 }
          _  = transaction.commit()
          _  = transaction.close()
          _ <- Logger[F].info(s"inserted ${chunk.size} departments")
        } yield ()
      }
    }
    .compile
    .drain

  private def insertEmployeesTypeDB(maxConcurrent: Int = 8): F[Unit] =
    getHrEmployeesStream
      .chunkN(1000)
      .parEvalMap(maxConcurrent) { chunk =>
        transactionManager.write(Domain.HR).use { transaction =>
          for {
            _ <- insertEmployeesChunk(chunk, transaction)
            _  = transaction.commit()
            _  = transaction.close()
            _ <- Logger[F].info(s"inserted ${chunk.size} employees")
          } yield ()
        }
      }
      .compile
      .drain

  private def insertEmployeesChunk(chunk: Chunk[Employee], transaction: TypeDBTransaction): F[Unit] =
    Async[F].blocking {
      chunk.map { e =>
        val hiredDate      = LocalDate.ofInstant(e.hiredDate, ZoneId.of("UTC"))
        val firedDate      = e.firedDate.fold(LocalDate.MIN)(LocalDate.ofInstant(_, ZoneId.of("UTC")))
        val insertQueryStr =
          s"""insert $$employee isa employee, has id "${e.id}", has job_id "${e.jobId}",
             |has department_id "${e.departmentId}", has gender ${e.gender}, has name "${e.name}", 
             |has email "${e.email}", has hired_date $hiredDate, has fired ${e.fired}, 
             |has fired_date $firedDate, has path "${e.path}";""".stripMargin
        val insertQuery    = TypeQL.parseQuery[TypeQLInsert](insertQueryStr)
        transaction.query().insert(insertQuery)
      }
    }.void

  private def insertJobsTypeDB() = getHrJobsStream
    .chunkN(1000)
    .parEvalMap(1) { chunk =>
      transactionManager.write(Domain.HR).use { transaction =>
        for {
          _ <- Async[F]
                 .delay {
                   chunk
                     .map { j =>
                       val insertQueryStr =
                         s"""insert $$job isa job, has id "${j.id}", has function_id "${j.functionId}", has code "${j.code}", has name "${j.name}";"""
                       val insertQuery    = TypeQL.parseQuery[TypeQLInsert](insertQueryStr)
                       transaction.query().insert(insertQuery)
                     }
                 }
          _  = transaction.commit()
          _  = transaction.close()
          _ <- Logger[F].info(s"inserted ${chunk.size} jobs")
        } yield ()
      }
    }
    .compile
    .drain

  private def insertJobFunctionsTypeDB() = getHrJobFunctionsStream
    .chunkN(1000)
    .parEvalMap(1) { chunk =>
      transactionManager.write(Domain.HR).use { transaction =>
        for {
          _ <- Async[F]
                 .delay {
                   chunk
                     .map { f =>
                       val insertQueryStr =
                         s"""insert $$job_function isa job_function, has id "${f.id}", has code "${f.code}", has name "${f.name}";"""
                       val insertQuery    = TypeQL.parseQuery[TypeQLInsert](insertQueryStr)
                       transaction.query().insert(insertQuery)
                     }
                 }
          _  = transaction.commit()
          _  = transaction.close()
          _ <- Logger[F].info(s"inserted ${chunk.size} job_functions")
        } yield ()
      }
    }
    .compile
    .drain

  private def insertRelations() = {
    val regionCities        = "match $region isa region, has id $id;$city isa city ,has region_id = $id;" +
      "insert $region_cities(region_role: $region, city_role: $city) isa region_cities;"
    val cityLocations       = "match $city isa city, has id $id;$location isa location ,has city_id = $id;" +
      "insert $city_locations(city_role: $city, location_role: $location) isa city_locations;"
    val locationDepartments =
      "match $location isa location, has id $id;$department isa department ,has location_id = $id;" +
        "insert $location_departments(location_role: $location, department_role: $department) isa location_departments;"
    val departmentEmployees =
      "match $department isa department, has id $id;$employee isa employee ,has department_id = $id;" +
        "insert $department_employees(department_role: $department, employee_role: $employee) isa department_employees;"
    val jobEmployees        = "match $job isa job, has id $id;$employee isa employee ,has job_id = $id;" +
      "insert $job_employees(job_role: $job, employee_role: $employee) isa job_employees;"
    val functionJobs        = "match $job_function isa job_function, has id $id;$job isa job ,has function_id = $id;" +
      "insert $function_jobs(function_role:$job_function,job_role: $job) isa function_jobs;"
    Vector(regionCities, cityLocations, locationDepartments, departmentEmployees, jobEmployees, functionJobs).foldMapA {
      insertQueryStr =>
        transactionManager.write(Domain.HR).use { transaction =>
          for {
            _ <- Async[F]
                   .blocking {
                     val insertQuery = TypeQL.parseQuery[TypeQLInsert](insertQueryStr)
                     transaction.query().insert(insertQuery)
                     transaction.commit()
                     transaction.close()
                   }
            _ <- Logger[F].info(s"inserted with $insertQueryStr")
          } yield ()
        }
    }
  }

  private def getHrRegionStream: Stream[F, Region] = {
    val sql = s"select * from $hrSchemaName.regions"
    Fragment(sql, List.empty).query[Region].stream.transact(xaStorage)
  }

  private def getHrCitiesStream: Stream[F, City] = {
    val sql = s"select * from $hrSchemaName.cities"
    Fragment(sql, List.empty).query[City].stream.transact(xaStorage)
  }

  private def getHrLocationsStream: Stream[F, Location] = {
    val sql = s"select * from $hrSchemaName.locations"
    Fragment(sql, List.empty).query[Location].stream.transact(xaStorage)
  }

  private def getHrDepartmentsStream: Stream[F, Department] = {
    val sql = s"select * from $hrSchemaName.departments"
    Fragment(sql, List.empty).query[Department].stream.transact(xaStorage)
  }

  private def getHrEmployeesStream: Stream[F, Employee] = {
    val sql = s"select * from $hrSchemaName.employees"
    Fragment(sql, List.empty).query[Employee].stream.transact(xaStorage)
  }

  private def getHrJobsStream: Stream[F, Job] = {
    val sql = s"select * from $hrSchemaName.jobs"
    Fragment(sql, List.empty).query[Job].stream.transact(xaStorage)
  }

  private def getHrJobFunctionsStream: Stream[F, JobFunction] = {
    val sql = s"select * from $hrSchemaName.job_functions"
    Fragment(sql, List.empty).query[JobFunction].stream.transact(xaStorage)
  }

  override def generateEmployees(n: Int): F[Unit] = {
    val departmentId = UUID.fromString("aebb311a-527b-11ee-be56-0242ac120009")
    val jobId        = UUID.fromString("5ddd0a88-527c-11ee-be56-0242ac120003")
    val path         = "employee1.employee8" // materialized path иерархия

    val sql = s"insert into $hrSchemaName..employees values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

    val chunkedEmployees = fs2.Stream.range(11, 11 + n).covary[F].chunkN(1000).map { chunk =>
      chunk.map(i =>
        Employee(
          id = UUID.randomUUID(),
          jobId = jobId,
          departmentId = departmentId,
          gender = if (Random.nextInt() % 2 == 0) true else false,
          name = s"employee$i",
          email = s"employee$i@mail.com",
          hiredDate = Instant.now(),
          fired = false,
          firedDate = None,
          path = s"$path.employee$i"
        )
      )
    }
    chunkedEmployees
      .evalMap { chunk =>
        for {
          insertMany <- Update[Employee](sql)
                          .updateMany(chunk)
                          .transact(xaStorage)
          _          <- Logger[F].info(s"chunk inserted: $insertMany")
        } yield ()
      }
      .compile
      .drain
  }

}
