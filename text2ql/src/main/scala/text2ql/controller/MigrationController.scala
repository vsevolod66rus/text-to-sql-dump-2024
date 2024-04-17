package text2ql.controller

import cats.effect.{Async, Resource}
import cats.implicits._
import sttp.tapir._
import sttp.tapir.json.circe.jsonBody
import text2ql.service.MigrationService
import text2ql.{Controller, EndpointError, WSEndpoint, baseEndpoint}

object MigrationController {

  def apply[F[_]: Async](
      migrationService: MigrationService[F]
  ): Resource[F, MigrationController[F]] = Resource.eval(
    Async[F].delay(
      new MigrationController[F](migrationService)
    )
  )
}

class MigrationController[F[_]: Async](
    migrationService: MigrationService[F]
) extends Controller[F] {

  private val adminBaseEndpoint = baseEndpoint.errorOut(jsonBody[EndpointError]).tag("Migrations")

  override val name = "Migrations"

  override def endpoints: Seq[WSEndpoint[F]] = Seq(
    adminBaseEndpoint.post
      .in("migration")
      .in("migrateHRDataToTypeDB")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertHrToTypeDB().map(_ => "migrated")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertCountries")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertCountries().map(_ => "inserted")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertCities")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertCities().map(_ => "inserted")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertDepartments")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertDepartments().map(_ => "inserted")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertJobFunctions")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertJobFunctions().map(_ => "inserted")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertJobs")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertJobs().map(_ => "inserted")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertHeads")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertHeadMembers().map(_ => "inserted")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertDepartmentsHeads")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertDepartmentsHeads().map(_ => "inserted")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertOthers")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertOtherEmployees().map(_ => "inserted")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("insertWoman")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertWomen().map(_ => "inserted")
      }
  )
}
