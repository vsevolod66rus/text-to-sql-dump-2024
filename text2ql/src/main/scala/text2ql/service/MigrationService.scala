package text2ql.service

import cats.effect.kernel._
import text2ql.dao.MigrationRepo

trait MigrationService[F[_]] {
  def generateEmployees(n: Int): F[Unit]
  def insertHrToTypeDB(): F[Unit]
}

object MigrationService {

  def apply[F[_]: Sync](
      migrationRepo: MigrationRepo[F]
  ): Resource[F, MigrationService[F]] =
    Resource.eval(Sync[F].delay(new MigrationServiceImpl(migrationRepo)))
}

class MigrationServiceImpl[F[_]](
    migrationRepo: MigrationRepo[F]
) extends MigrationService[F] {
  override def generateEmployees(n: Int): F[Unit] = migrationRepo.generateEmployees(n)
  override def insertHrToTypeDB(): F[Unit]        = migrationRepo.insertHrToTypeDB()
}
