package text2ql.configs

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

case class ApplicationConfig(
    database: DBConfig
)

object ApplicationConfig {
  implicit val reader: ConfigReader[ApplicationConfig] = deriveReader
}
