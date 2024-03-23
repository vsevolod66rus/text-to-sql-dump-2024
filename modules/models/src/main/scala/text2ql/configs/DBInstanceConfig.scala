package text2ql.configs

import pureconfig.ConfigReader
import pureconfig.configurable.genericMapReader
import pureconfig.error.ExceptionThrown
import pureconfig.generic.semiauto.deriveReader
import text2ql.api.Domain

case class DBConfig(
    storage: DBInstanceConfig,
    data: DBDataConfig
)

case class DBInstanceConfig(
    url: String,
    username: String,
    password: String,
    connectionThreads: Int,
    name: String
)

case class DBDataConfig(
    pgSchemas: Map[Domain, String],
    useCursor: Boolean = true
)

object DBConfig {
  implicit val readerDB: ConfigReader[DBConfig]                 = deriveReader
  implicit val readerDBInstance: ConfigReader[DBInstanceConfig] = deriveReader
  implicit val readerDBData: ConfigReader[DBDataConfig]         = deriveReader

  implicit def mapReader[V: ConfigReader]: ConfigReader[Map[Domain, V]] = genericMapReader(
    Domain.withNameInsensitiveEither(_).left.map(ExceptionThrown)
  )
}
