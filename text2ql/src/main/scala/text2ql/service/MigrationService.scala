package text2ql.service

import cats.effect.kernel._
import text2ql.dao.MigrationRepo

trait MigrationService[F[_]] {
  def insertHrToTypeDB(): F[Unit]
  def insertCountries(): F[Unit]
  def insertCities(): F[Unit]
  def insertDepartments(): F[Unit]
  def insertJobFunctions(): F[Unit]
  def insertJobs(): F[Unit]
  def insertHeadMembers(): F[Unit]
  def insertDepartmentsHeads(): F[Unit]
  def insertOtherEmployees(): F[Unit]
  def insertWomen(): F[Unit]
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
  override def insertHrToTypeDB(): F[Unit] = migrationRepo.insertHrToTypeDB()
  def insertCountries(): F[Unit]           = migrationRepo.insertCountries()
  def insertCities(): F[Unit]              = migrationRepo.insertCities()
  def insertDepartments(): F[Unit]         = migrationRepo.insertDepartments()
  def insertJobFunctions(): F[Unit]        = migrationRepo.insertJobFunctions()
  def insertJobs(): F[Unit]                = migrationRepo.insertJobs()
  def insertHeadMembers(): F[Unit]         = migrationRepo.insertHeadMembers()
  def insertDepartmentsHeads(): F[Unit]    = migrationRepo.insertDepartmentsHeads()
  def insertOtherEmployees(): F[Unit]      = migrationRepo.insertOtherEmployees()
  def insertWomen(): F[Unit] = migrationRepo.insertWomen()
}
