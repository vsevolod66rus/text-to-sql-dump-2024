package text2ql.configs

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

case class TypeDBConfig(
    url: String,
    dbHR: String,
    rules: Boolean,
    limit: Int,
    parallel: Boolean,
    transactionTimeoutMillis: Int,
    maxConcurrentTypeDB: Int,
    limitRetries: Int,
    constantDelay: Int
)

object TypeDBConfig {
  implicit val reader: ConfigReader[TypeDBConfig] = deriveReader
}
