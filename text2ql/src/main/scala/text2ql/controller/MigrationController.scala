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
      .in("generateEmployees")
      .in(path[Int]("n"))
      .out(stringBody)
      .serverLogicRecoverErrors { n =>
        migrationService.generateEmployees(n).map(_ => "generated")
      },
    adminBaseEndpoint.post
      .in("migration")
      .in("migrateHRDataToTypeDB")
      .out(stringBody)
      .serverLogicRecoverErrors { _ =>
        migrationService.insertHrToTypeDB().map(_ => "migrated")
      }
  )
}
