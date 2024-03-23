package text2ql.configs

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

case class ServerConfig(
    host: String,
    port: Int
)

object ServerConfig {
  implicit val reader: ConfigReader[ServerConfig] = deriveReader
}
