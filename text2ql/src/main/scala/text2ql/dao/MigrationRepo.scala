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
  def generateEmployees(): F[Unit]
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

object MigrationRepo {

  def apply[F[_]: Async: Logger](
      xaStorage: Transactor[F],
      conf: DBDataConfig
  ): Resource[F, MigrationRepo[F]] =
    Resource.eval(Sync[F].delay(new MigrationRepoImpl(xaStorage, conf)))
}

class MigrationRepoImpl[F[_]: Async: Logger](
    xaStorage: Transactor[F],
    conf: DBDataConfig
) extends MigrationRepo[F] {

  def insertHrToTypeDB(): F[Unit] = ().pure[F]

  private val hrSchemaName = conf.pgSchemas.getOrElse(Domain.HR, Domain.HR.entryName)

  sealed trait DBEntity                                                               extends Product with Serializable
  private case class Country(id: UUID, name: String)                                  extends DBEntity
  private case class City(id: UUID, countryId: UUID, name: String)                    extends DBEntity
  private case class Department(id: UUID, cityId: UUID, name: String, parentId: UUID) extends DBEntity
  private case class JobFunction(id: UUID, name: String)                              extends DBEntity
  private case class Job(id: UUID, functionId: UUID, name: String)                    extends DBEntity

  private case class Employee(
      id: UUID,
      jobId: UUID,
      departmentId: UUID,
      gender: String,
      age: Int,
      height: Int,
      fullName: String,
      citizenship: String,
      salary: Double,
      email: String,
      hiredDate: Instant,
      fired: Boolean,
      firedDate: Option[Instant],
      parentId: Option[UUID]
  ) extends DBEntity

  private lazy val countries = Vector(Country(id = UUID.randomUUID(), name = "Русь"))

  private lazy val cities = Vector(
    City(id = UUID.randomUUID(), countryId = UUID.fromString("151a31da-c350-4296-9e63-21db586dee83"), name = "Москва"),
    City(
      id = UUID.randomUUID(),
      countryId = UUID.fromString("151a31da-c350-4296-9e63-21db586dee83"),
      name = "Екатеринбург"
    ),
    City(id = UUID.randomUUID(), countryId = UUID.fromString("151a31da-c350-4296-9e63-21db586dee83"), name = "Асгард"),
    City(
      id = UUID.randomUUID(),
      countryId = UUID.fromString("151a31da-c350-4296-9e63-21db586dee83"),
      name = "Санкт-Петербург"
    ),
    City(id = UUID.randomUUID(), countryId = UUID.fromString("151a31da-c350-4296-9e63-21db586dee83"), name = "Милан")
  )

  private lazy val departments = Vector(
    Department(
      id = UUID.randomUUID(),
      cityId = UUID.fromString("ba7adf4c-2dbe-444e-a14d-8bcafd895ce1"),
      name = "Департамент логистики",
      parentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93")
    ),
    Department(
      id = UUID.randomUUID(),
      cityId = UUID.fromString("ea043a43-fa09-4c67-81cb-035a2aab1f67"),
      name = "Департамент сельского хозяйства",
      parentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93")
    ),
    Department(
      id = UUID.randomUUID(),
      cityId = UUID.fromString("6ce2ea71-a255-483f-b569-3a15354ab2b1"),
      name = "Департамент обороны и нападения",
      parentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93")
    ),
    Department(
      id = UUID.randomUUID(),
      cityId = UUID.fromString("48da8bb9-0ac0-4a32-809b-5d12cba46c10"),
      name = "Департамент информационных технологий",
      parentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93")
    ),
    Department(
      id = UUID.randomUUID(),
      cityId = UUID.fromString("e5e4b5a0-f857-4840-887b-95f30d642c44"),
      name = "Департамент научных исследований",
      parentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93")
    ),
    Department(
      id = UUID.randomUUID(),
      cityId = UUID.fromString("e5e4b5a0-f857-4840-887b-95f30d642c44"),
      name = "Департамент здравоохранения",
      parentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93")
    ),
    Department(
      id = UUID.randomUUID(),
      cityId = UUID.fromString("ea043a43-fa09-4c67-81cb-035a2aab1f67"),
      name = "Департамент тяжелой промышленности",
      parentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93")
    )
  )

  private lazy val jobFunctions = Vector(
    JobFunction(id = UUID.randomUUID(), name = "Оборона и нападение"),
    JobFunction(id = UUID.randomUUID(), name = "Разведка"),
    JobFunction(id = UUID.randomUUID(), name = "Разработка ПО"),
    JobFunction(id = UUID.randomUUID(), name = "Научные исследования"),
    JobFunction(id = UUID.randomUUID(), name = "Земледелие и скотоводство"),
    JobFunction(id = UUID.randomUUID(), name = "Терапия"),
    JobFunction(id = UUID.randomUUID(), name = "Хирургия"),
    JobFunction(id = UUID.randomUUID(), name = "Грузоперевозки"),
    JobFunction(id = UUID.randomUUID(), name = "Менеджмент"),
    JobFunction(id = UUID.randomUUID(), name = "Финансовые операции"),
    JobFunction(id = UUID.randomUUID(), name = "Юридическое сопровождение"),
    JobFunction(id = UUID.randomUUID(), name = "Машиностроение")
  )

  private lazy val jobs = Vector(
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("7b3faba5-3529-47e7-8f13-3511875609e8"),
      name = "Водитель"
    ),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("7b3faba5-3529-47e7-8f13-3511875609e8"), name = "Грузчик"),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("9cdbc1ab-d0cb-4540-886c-fefa8347a111"),
      name = "Тракторист"
    ),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("9cdbc1ab-d0cb-4540-886c-fefa8347a111"), name = "Агроном"),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("68fd0d63-cf20-43da-8f61-3ddd05995c16"),
      name = "Инженер-конструктор"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("68fd0d63-cf20-43da-8f61-3ddd05995c16"),
      name = "Оператор ЧПУ"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("6b004239-39c1-4fd3-914d-46ea2affcf88"),
      name = "Руководитель департамента"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("6b004239-39c1-4fd3-914d-46ea2affcf88"),
      name = "Член совета директоров"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("0f721c42-6485-4070-9a9c-8a4ec585b56a"),
      name = "Инженер-исследователь"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("0f721c42-6485-4070-9a9c-8a4ec585b56a"),
      name = "Старший научный сотрудник"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("512075d8-0c93-47b6-b7a6-0d00e8fb4dca"),
      name = "Оператор БПЛА"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("512075d8-0c93-47b6-b7a6-0d00e8fb4dca"),
      name = "Штурмовик"
    ),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("512075d8-0c93-47b6-b7a6-0d00e8fb4dca"), name = "Механик"),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("512075d8-0c93-47b6-b7a6-0d00e8fb4dca"), name = "Связист"),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("512075d8-0c93-47b6-b7a6-0d00e8fb4dca"),
      name = "Оператор гиперзвуковых ракет"
    ),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("512075d8-0c93-47b6-b7a6-0d00e8fb4dca"), name = "Снайпер"),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("fcb96806-33c2-4b5c-af2b-3295ba9c45cf"),
      name = "Разведчик"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("3b414ced-a1de-4eb0-b788-7f84f7192183"),
      name = "Scala-разработчик"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("3b414ced-a1de-4eb0-b788-7f84f7192183"),
      name = "Scrum-♂master♂"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("b866920e-b6a7-4b9a-99bb-cd7dbeeb5a2d"),
      name = "Терапевт"
    ),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("b866920e-b6a7-4b9a-99bb-cd7dbeeb5a2d"), name = "Медбрат"),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("29e23e46-d1c0-4684-9462-9b32f701240c"),
      name = "Финансист"
    ),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("29e23e46-d1c0-4684-9462-9b32f701240c"),
      name = "Криптотрейдер"
    ),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("9587d56b-aec1-4501-9c07-6b3dcdace214"), name = "Хирург"),
    Job(
      id = UUID.randomUUID(),
      functionId = UUID.fromString("9587d56b-aec1-4501-9c07-6b3dcdace214"),
      name = "Анестезиолог"
    ),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("5c203c0f-1fee-491c-94b0-886f470fb3fe"), name = "Адвокат"),
    Job(id = UUID.randomUUID(), functionId = UUID.fromString("5c203c0f-1fee-491c-94b0-886f470fb3fe"), name = "Юрист")
  )

  private lazy val membersHead = Vector
    .fill(4)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("09ef88fc-7a16-48e7-a3f2-f77dbf50615b"),
        departmentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93"),
        gender = "M",
        age = Random.between(50, 70),
        height = Random.between(160, 190),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(5000000d, 8000000d),
        email = s"${fullName.replaceAll(" ", "")}${Random.between(0, 1000)}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738000),
        fired = false,
        firedDate = None,
        parentId = None
      )
    )

  private lazy val departmentsHead = Vector
    .fill(8)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("228c9d0f-ae12-4b7f-8c7b-27688f956d87"),
        departmentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93"),
        gender = "M",
        age = Random.between(40, 70),
        height = Random.between(160, 195),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(1000000d, 2000000d),
        email = s"${fullName.replaceAll(" ", "")}${Random.between(0, 1000)}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738001),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("e56cae5d-fb6b-4a49-986f-092fc8e078ed"))
      )
    )

  private lazy val lawers = Vector
    .fill(20)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("78355d6b-45e2-49b1-acdb-5dfcc8579ab7"),
        departmentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93"),
        gender = "M",
        age = Random.between(35, 70),
        height = Random.between(160, 195),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(300000d, 1000000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738002),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("e36a2bf9-19e1-4bf4-9751-0cc1f5c5bc05"))
      )
    )

  private lazy val financess = Vector
    .fill(20)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("21183f41-f985-4caa-a928-e0871bc38f18"),
        departmentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93"),
        gender = "M",
        age = Random.between(35, 70),
        height = Random.between(160, 195),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(400000d, 1000000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738003),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("e36a2bf9-19e1-4bf4-9751-0cc1f5c5bc05"))
      )
    )

  private lazy val scalas = Vector
    .fill(200)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("cd2c5c92-74ce-4c8b-83cd-564eae48275d"),
        departmentId = UUID.fromString("ddbf5fe5-5780-44f7-9259-436f2e706c26"),
        gender = "M",
        age = Random.between(20, 40),
        height = Random.between(175, 195),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(600000d, 1000000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738004),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("1c4df837-63e1-4b32-8e70-5237df8f5c08"))
      )
    )

  private lazy val scrums = Vector
    .fill(200)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("09b8862f-06b9-4873-baa9-239a138602f5"),
        departmentId = UUID.fromString("ddbf5fe5-5780-44f7-9259-436f2e706c26"),
        gender = "M",
        age = Random.between(20, 50),
        height = Random.between(170, 180),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(50000d, 100000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738005),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("1c4df837-63e1-4b32-8e70-5237df8f5c08"))
      )
    )

  private lazy val agronoms = Vector
    .fill(300)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("07e9d893-ac5d-4ab9-a134-ad05352a4a72"),
        departmentId = UUID.fromString("b2e2a9fc-0150-4098-9704-73f2c538da40"),
        gender = "M",
        age = Random.between(20, 50),
        height = Random.between(170, 185),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(100000d, 300000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738006),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("61032385-14b9-4dc0-91e7-e5942f6a6bfe"))
      )
    )

  private lazy val tractors = Vector
    .fill(300)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("817c8136-4d8c-4cf7-9fdd-eb3cee24dd4b"),
        departmentId = UUID.fromString("b2e2a9fc-0150-4098-9704-73f2c538da40"),
        gender = "M",
        age = Random.between(20, 50),
        height = Random.between(170, 185),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(210000d, 320000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738007),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("61032385-14b9-4dc0-91e7-e5942f6a6bfe"))
      )
    )

  private lazy val advocates = Vector
    .fill(30)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("79b9aa23-cc24-41a1-8482-99eedc34dcbc"),
        departmentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93"),
        gender = "M",
        age = Random.between(20, 50),
        height = Random.between(170, 185),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(300000d, 590000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738008),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("e36a2bf9-19e1-4bf4-9751-0cc1f5c5bc05"))
      )
    )

  private lazy val traiders = Vector
    .fill(15)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("09e02a77-3646-44c2-943b-32463c4e3306"),
        departmentId = UUID.fromString("83aa97ed-5cb1-4589-abb2-5470297faf93"),
        gender = "M",
        age = Random.between(20, 40),
        height = Random.between(160, 175),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(400000d, 700000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738009),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("e36a2bf9-19e1-4bf4-9751-0cc1f5c5bc05"))
      )
    )

  private lazy val anestaziologs = Vector
    .fill(50)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("6cbecf9d-ff6c-4624-8551-075dc7c85fa9"),
        departmentId = UUID.fromString("a4583bb1-74ea-4f8b-8e7a-71c8648a43ac"),
        gender = "M",
        age = Random.between(20, 35),
        height = Random.between(165, 180),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(100000d, 200000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738010),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("813bd67e-c023-45c7-bf37-9f447337d6df"))
      )
    )

  private lazy val surgeons = Vector
    .fill(36)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("985ea32e-6ed8-4840-ae4e-ddc24f166476"),
        departmentId = UUID.fromString("a4583bb1-74ea-4f8b-8e7a-71c8648a43ac"),
        gender = "M",
        age = Random.between(30, 65),
        height = Random.between(170, 185),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(400000d, 600000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738011),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("813bd67e-c023-45c7-bf37-9f447337d6df"))
      )
    )

  private lazy val medBrothers = Vector
    .fill(107)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("27e658bc-b991-4f12-96dc-fe8313fc7b36"),
        departmentId = UUID.fromString("a4583bb1-74ea-4f8b-8e7a-71c8648a43ac"),
        gender = "M",
        age = Random.between(20, 45),
        height = Random.between(170, 180),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(100000d, 150000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738012),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("813bd67e-c023-45c7-bf37-9f447337d6df"))
      )
    )

  private lazy val terapevts = Vector
    .fill(78)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("180c2081-2fa1-4971-8489-277f254d243c"),
        departmentId = UUID.fromString("a4583bb1-74ea-4f8b-8e7a-71c8648a43ac"),
        gender = "M",
        age = Random.between(30, 45),
        height = Random.between(170, 183),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(150000d, 250000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738013),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("813bd67e-c023-45c7-bf37-9f447337d6df"))
      )
    )

  private lazy val drvers = Vector
    .fill(990)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("43854dd6-05dc-4d67-b014-cd2be5453705"),
        departmentId = UUID.fromString("2e707ab4-4e7e-447c-9f7e-d4f919f2da67"),
        gender = "M",
        age = Random.between(23, 45),
        height = Random.between(170, 190),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(100000d, 250000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738014),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("a03bd3ca-f4c0-40ab-afdf-ad67a708d4eb"))
      )
    )

  private lazy val loaders = Vector
    .fill(700)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("d40b547d-fc87-4a61-b36a-35695c43af99"),
        departmentId = UUID.fromString("2e707ab4-4e7e-447c-9f7e-d4f919f2da67"),
        gender = "M",
        age = Random.between(20, 43),
        height = Random.between(160, 195),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(100000d, 190000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738015),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("a03bd3ca-f4c0-40ab-afdf-ad67a708d4eb"))
      )
    )

  private lazy val sciEngiiners = Vector
    .fill(206)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("6a1133a2-aa00-4156-a130-58c5c459b5ab"),
        departmentId = UUID.fromString("dd082f88-4650-47ca-b3bc-559af9ce2be7"),
        gender = "M",
        age = Random.between(25, 60),
        height = Random.between(169, 195),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(200000d, 490000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738016),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("5471fd3a-d2aa-43cc-b259-2d56a540c72f"))
      )
    )

  private lazy val sciEmployees = Vector
    .fill(402)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("d37914cf-6a54-482f-8a3b-333d6c464464"),
        departmentId = UUID.fromString("dd082f88-4650-47ca-b3bc-559af9ce2be7"),
        gender = "M",
        age = Random.between(24, 65),
        height = Random.between(165, 195),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(150000d, 390000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738017),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("5471fd3a-d2aa-43cc-b259-2d56a540c72f"))
      )
    )

  private lazy val factoryEngiiners = Vector
    .fill(345)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("067de3c2-bf12-495e-92a8-b2345fd78577"),
        departmentId = UUID.fromString("8495c940-0fac-4c46-b6e0-3d7c0437be22"),
        gender = "M",
        age = Random.between(30, 65),
        height = Random.between(161, 186),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(150000d, 290000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738018),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("653f6bf4-c453-4b9b-ada9-da1f0bce703b"))
      )
    )

  private lazy val factoryOperators = Vector
    .fill(752)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("2178ef71-36bb-43c3-b301-9653317eb951"),
        departmentId = UUID.fromString("8495c940-0fac-4c46-b6e0-3d7c0437be22"),
        gender = "M",
        age = Random.between(19, 65),
        height = Random.between(161, 193),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(130000d, 230000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738019),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("653f6bf4-c453-4b9b-ada9-da1f0bce703b"))
      )
    )

  private lazy val storms = Vector
    .fill(17000)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("ffd3ce7d-4d65-4fac-9ad6-ccabe41b1158"),
        departmentId = UUID.fromString("c3e5ebdc-4351-46b7-bc55-d815def6da3a"),
        gender = "M",
        age = Random.between(18, 40),
        height = Random.between(161, 180),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(200000d, 330000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738020),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e"))
      )
    )

  private lazy val snipers = Vector
    .fill(150)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("d03e8842-d3fd-4ca0-99f1-631f57a0d744"),
        departmentId = UUID.fromString("c3e5ebdc-4351-46b7-bc55-d815def6da3a"),
        gender = "M",
        age = Random.between(25, 50),
        height = Random.between(161, 175),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(400000d, 800000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738021),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e"))
      )
    )

  private lazy val raketas = Vector
    .fill(250)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("d233aa59-a9bb-45ca-a9cb-6c1060c23fbd"),
        departmentId = UUID.fromString("c3e5ebdc-4351-46b7-bc55-d815def6da3a"),
        gender = "M",
        age = Random.between(23, 45),
        height = Random.between(167, 187),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(200000d, 300000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738022),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e"))
      )
    )

  private lazy val signalmans = Vector
    .fill(950)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("35fc1fef-0df3-4f61-9cc7-9a810c34797a"),
        departmentId = UUID.fromString("c3e5ebdc-4351-46b7-bc55-d815def6da3a"),
        gender = "M",
        age = Random.between(23, 45),
        height = Random.between(167, 187),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(200000d, 300000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738023),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e"))
      )
    )

  private lazy val BPLas = Vector
    .fill(700)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("55bdfcc7-22a6-40ab-9963-c9cff7635f04"),
        departmentId = UUID.fromString("c3e5ebdc-4351-46b7-bc55-d815def6da3a"),
        gender = "M",
        age = Random.between(23, 45),
        height = Random.between(167, 192),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(200000d, 300000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738024),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e"))
      )
    )

  private lazy val mechs = Vector
    .fill(502)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("c39b9d5f-7b9c-49cf-9127-93446127f30a"),
        departmentId = UUID.fromString("c3e5ebdc-4351-46b7-bc55-d815def6da3a"),
        gender = "M",
        age = Random.between(23, 53),
        height = Random.between(167, 192),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(200000d, 300000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738025),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e"))
      )
    )

  private lazy val razved = Vector
    .fill(303)(generateFIO())
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("977a663b-0a01-497c-9f41-4e82fd23a57e"),
        departmentId = UUID.fromString("c3e5ebdc-4351-46b7-bc55-d815def6da3a"),
        gender = "M",
        age = Random.between(19, 75),
        height = Random.between(165, 189),
        fullName = fullName,
        citizenship = "Русский",
        salary = Random.between(300000d, 900000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738026),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("aebc1e7f-bda4-4107-8a0e-a4043aa5cd0e"))
      )
    )

  private lazy val womans = Vector
    .fill(402)(generateFIO(true))
    .map(fullName =>
      Employee(
        id = UUID.randomUUID(),
        jobId = UUID.fromString("b866920e-b6a7-4b9a-99bb-cd7dbeeb5228"),
        departmentId = UUID.fromString("a4583bb1-74ea-4f8b-8e7a-71c8648a43ac"),
        gender = "Ж",
        age = Random.between(18, 35),
        height = Random.between(160, 180),
        fullName = fullName,
        citizenship = "Русская",
        salary = Random.between(100000d, 300000d),
        email = s"${fullName.replaceAll(" ", "")}${UUID.randomUUID().hashCode()}@yandex.ru",
        hiredDate = Instant.ofEpochSecond(693738027),
        fired = false,
        firedDate = None,
        parentId = Some(UUID.fromString("813bd67e-c023-45c7-bf37-9f447337d6df"))
      )
    )

  def insertCountries(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.countries values (?, ?)"
    for {
      insertMany <- Update[Country](sql)
                      .updateMany(countries)
                      .transact(xaStorage)
      _          <- Logger[F].info(s"countries inserted: $insertMany")
    } yield ()
  }

  def insertCities(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.cities values (?, ?, ?)"
    for {
      insertMany <- Update[City](sql)
                      .updateMany(cities)
                      .transact(xaStorage)
      _          <- Logger[F].info(s"cities inserted: $insertMany")
    } yield ()
  }

  def insertDepartments(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.departments values (?, ?, ?, ?)"
    for {
      insertMany <- Update[Department](sql)
                      .updateMany(departments)
                      .transact(xaStorage)
      _          <- Logger[F].info(s"departments inserted: $insertMany")
    } yield ()
  }

  def insertJobFunctions(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.job_functions values (?, ?)"
    for {
      insertMany <- Update[JobFunction](sql)
                      .updateMany(jobFunctions)
                      .transact(xaStorage)
      _          <- Logger[F].info(s"job functions inserted: $insertMany")
    } yield ()
  }

  def insertJobs(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.jobs values (?, ?, ?)"
    for {
      insertMany <- Update[Job](sql)
                      .updateMany(jobs)
                      .transact(xaStorage)
      _          <- Logger[F].info(s"jobs inserted: $insertMany")
    } yield ()
  }

  def generateEmployees(): F[Unit] = Sync[F].unit

  def insertHeadMembers(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.employees values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    for {
      insertMany <- Update[Employee](sql)
                      .updateMany(membersHead)
                      .transact(xaStorage)
      _          <- Logger[F].info(s"heads inserted: $insertMany")
    } yield ()
  }

  def insertDepartmentsHeads(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.employees values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    for {
      insertMany <- Update[Employee](sql)
                      .updateMany(departmentsHead)
                      .transact(xaStorage)
      _          <- Logger[F].info(s"departments heads inserted: $insertMany")
    } yield ()
  }

  def insertWomen(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.employees values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    for {
      insertMany <- Update[Employee](sql)
                      .updateMany(womans)
                      .transact(xaStorage)
      _          <- Logger[F].info(s"womans inserted: $insertMany")
    } yield ()
  }

  def insertOtherEmployees(): F[Unit] = {
    val sql = s"insert into $hrSchemaName.employees values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    for {
      insertMany <-
        Update[Employee](sql)
          .updateMany(
            lawers ++
              financess ++
              scalas ++
              scrums ++
              agronoms ++
              tractors ++
              advocates ++
              traiders ++
              anestaziologs ++
              surgeons ++
              medBrothers ++
              terapevts ++
              drvers ++
              loaders ++
              sciEngiiners ++
              sciEmployees ++
              factoryEngiiners ++
              factoryOperators ++
              storms ++
              snipers ++
              raketas ++
              signalmans ++
              BPLas ++
              mechs ++
              razved
          )
          .transact(xaStorage)
      _          <- Logger[F].info(s"employees heads inserted: $insertMany")
    } yield ()
  }

  private def generateFIO(woman: Boolean = false): String = {
    val names        = Vector(
      "Акамир",
      "Бажен",
      "Белослав",
      "Богдан",
      "Богомил",
      "Борис",
      "Бранимир",
      "Боромир",
      "Арагорн",
      "Адольф",
      "Братислав",
      "Будимир",
      "Велиград",
      "Олег",
      "Верослав",
      "Владимир",
      "Всеволод",
      "Волк",
      "Всеслав",
      "Вячеслав",
      "Градислав",
      "Добрын",
      "Добромил",
      "Доброслав",
      "Драгомир",
      "Душан",
      "Жирослав",
      "Истислав",
      "Изяслав",
      "Казимир",
      "Ладимир",
      "Маломир",
      "Милан",
      "Милован",
      "Милорад",
      "Мирослав",
      "Мстислав",
      "Остромир",
      "Осмомысл",
      "Пересвет",
      "Премысл",
      "Радомысл",
      "Ратмир",
      "Ростислав",
      "Святополк",
      "Святослав",
      "Славомир",
      "Станислав",
      "Тихомир",
      "Яромир",
      "Ярополк",
      "Ярослав",
      "Джокер",
      "Джеймс",
      "Райан",
      "Джейсон",
      "Билл",
      "Готикаслав",
      "Русослав",
      "Гомобор",
      "Гетерослав",
      "Уралоид",
      "Слонослав",
      "Медведослав",
      "Кабанослав",
      "Всеволод",
      "Александр",
      "Демид",
      "Донбасослав",
      "Крымослав",
      "Михаил",
      "Светозар"
    )
    val soNames      = Vector(
      "Стетхем",
      "Гослинг",
      "Даркхолм",
      "Херрингтон",
      "Дикий",
      "Резвый",
      "Светлый",
      "Мудрый",
      "Никитин",
      "Иванов",
      "Петров",
      "Сидоров",
      "Кузнецов",
      "Романов",
      "Кабанов",
      "Макэвой",
      "Светов",
      "Ельцин",
      "Солнцев",
      "Токарев",
      "Гусев",
      "Волков",
      "Назаров",
      "Светов",
      "Эйнштейн",
      "Поздняков",
      "Королев",
      "Годунов",
      "Милос",
      "Хохлов"
    )
    val namesWoman   = Vector(
      "Наталья",
      "Светлана",
      "Оксана",
      "Каролина",
      "Ирина",
      "Стелла",
      "Елена",
      "Мария",
      "Маргарет",
      "Джессика"
    )
    val soNamesWoman = soNames
      .filter(s => s.endsWith("ев") || s.endsWith("ов"))
      .map(_ + "a")
    val res          = if (woman) {

      namesWoman
        .get(Random.between(0, namesWoman.size))
        .getOrElse("") + " " +
        names
          .get(Random.between(0, names.size))
          .getOrElse("") + "овна" + " " +
        soNamesWoman
          .get(Random.between(0, soNamesWoman.size))
          .getOrElse("")
    } else {
      names
        .get(Random.between(0, names.size))
        .getOrElse("") + " " +
        names
          .get(Random.between(0, names.size))
          .getOrElse("") + "ович" + " " +
        soNames
          .get(Random.between(0, soNames.size))
          .getOrElse("")
    }
    res
  }

}
