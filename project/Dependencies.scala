import sbt._

object Dependencies {

  object Versions {
    lazy val apiSpecVersion          = "0.4.0"
    lazy val betterMonadicForVersion = "0.3.1"
    lazy val catsEffectVersion       = "3.3.12"
    lazy val catsVersion             = "2.9.0"
    lazy val circeVersion            = "0.14.1"
    lazy val circeFs2Version         = "0.14.1"
    lazy val circeYamlVersion        = "0.14.1"
    lazy val enumeratumCirceVersion  = "1.7.0"
    lazy val jwtCirceVersion         = "9.0.5"
    lazy val doobieVersion           = "1.0.0-RC1"
    lazy val quillVersion            = "3.6.0"
    lazy val fs2Version              = "3.6.1"
    lazy val http4sVersion           = "0.23.12"
    lazy val kindProjectorVersion    = "0.13.2"
    lazy val liquibaseVersion        = "4.12.0"
    lazy val logbackVersion          = "1.4.6"
    lazy val prometheusVersion       = "0.10.0"
    lazy val pureconfigVersion       = "0.17.1"
    lazy val slf4jVersion            = "2.0.5"
    lazy val sttpClient3Version      = "3.8.13"
    lazy val sttpVersion             = "1.7.2"
    lazy val tapirVersion            = "1.5.0"
    lazy val chimneyVersion          = "0.6.1"
    lazy val typeDBVersion           = "2.18.1"
    lazy val jmorphy2Version         = "0.2.0-SNAPSHOT"
    lazy val uuidCreatorVersion      = "3.7.3"
    lazy val scalaCacheVersion       = "1.0.0-M6"
    lazy val catsRetryVersion        = "3.1.0"
    lazy val keyCloakVersion         = "18.0.2"
    lazy val tsecVersion             = "0.4.0"
    lazy val apacheHttpClientVersion = "4.5.13"
    lazy val circuitVersion          = "0.5.0"
    lazy val jodaVersion             = "2.10.13"
    lazy val scalaLoggingVersion     = "3.9.3"
    lazy val magnoliaVersion         = "1.1.2"
    lazy val weaverVersion           = "0.8.1"
    lazy val munitVersion            = "1.0.7"
    lazy val mockitoVersion          = "1.17.7"
    lazy val fs2cronVersion          = "0.8.3"
    lazy val scalaParserCombsVersion = "2.2.0"
    lazy val csvReaderVersion        = "1.3.10"
    lazy val toFuVersion             = "0.12.0.1"
    lazy val clickhouseJdbcVersion   = "0.4.6"
    lazy val log4CatsVersion         = "2.5.0"
  }

  object Resolvers {

    lazy val resolvers = Seq(
      "repo.vaticle.com".at("https://repo.vaticle.com/repository/maven/")
    )
  }
  import Versions._

  lazy val kindProjector = ("org.typelevel" %% "kind-projector" % kindProjectorVersion).cross(CrossVersion.full)

  lazy val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % betterMonadicForVersion

  lazy val cats          = "org.typelevel" %% "cats-core"       % catsVersion
  lazy val catsEffect    = "org.typelevel" %% "cats-effect"     % catsEffectVersion
  lazy val catsEffectStd = "org.typelevel" %% "cats-effect-std" % catsEffectVersion

  lazy val fs2 = "co.fs2" %% "fs2-core" % fs2Version

  lazy val circe     = Seq(
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-generic"
  ).map(_ % circeVersion)

  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion

  lazy val circeFs2 = "io.circe" %% "circe-fs2" % circeFs2Version

  lazy val circeYaml = "io.circe" %% "circe-yaml" % circeYamlVersion

  lazy val circeEnumeratum = "com.beachape" %% "enumeratum-circe" % enumeratumCirceVersion
  lazy val quillEnumeratum = "com.beachape" %% "enumeratum-quill" % enumeratumCirceVersion

  lazy val circeGenericExtras = "io.circe" %% "circe-generic-extras" % circeVersion

  lazy val jwtCirce = "com.github.jwt-scala" %% "jwt-circe" % jwtCirceVersion

  lazy val chimney = "io.scalaland" %% "chimney" % chimneyVersion

  lazy val http4s      = Seq(
    "org.http4s" %% "http4s-dsl",
    "org.http4s" %% "http4s-blaze-server",
    "org.http4s" %% "http4s-blaze-client"
  ).map(_ % http4sVersion)

  lazy val tapir       = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
    "com.softwaremill.sttp.tapir" %% "tapir-enumeratum"
  ).map(_ % tapirVersion)

  lazy val tapirServer = Seq(
    "com.softwaremill.sttp.tapir"   %% "tapir-openapi-docs"      % tapirVersion,
    "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server"     % tapirVersion,
    "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle" % tapirVersion,
    "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"      % apiSpecVersion
  )

  lazy val tapirCore = "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion

  lazy val tapirSttpClient = "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion

  lazy val tapirRefined = "com.softwaremill.sttp.tapir" %% "tapir-refined" % tapirVersion

  lazy val tapirSttpStubServer = "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion

  lazy val sttpCore = "com.softwaremill.sttp" %% "core" % sttpVersion

  lazy val sttpClient3Core = "com.softwaremill.sttp.client3" %% "core" % sttpClient3Version

  lazy val sttpClient3Http4sBackend = "com.softwaremill.sttp.client3" %% "http4s-backend" % sttpClient3Version

  lazy val http4sCirce = "org.http4s" %% "http4s-circe" % http4sVersion

  lazy val sttpClient3Cats = "com.softwaremill.sttp.client3" %% "cats" % sttpClient3Version

  lazy val doobie    = Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-postgres",
    "org.tpolecat" %% "doobie-specs2",
    "org.tpolecat" %% "doobie-hikari",
    "org.tpolecat" %% "doobie-quill",
    "org.tpolecat" %% "doobie-refined",
    "org.tpolecat" %% "doobie-postgres-circe"
  ).map(_ % doobieVersion)

  lazy val liquibase = "org.liquibase" % "liquibase-core" % liquibaseVersion

  lazy val prometheusHttp4s = "org.http4s" %% "http4s-prometheus-metrics" % http4sVersion

  lazy val prometheusTapir = "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion

  lazy val pureconfig = "com.github.pureconfig" %% "pureconfig" % pureconfigVersion

  lazy val pureconfigCE = "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfigVersion

  lazy val pureConfigEnumeratum = "com.github.pureconfig" %% "pureconfig-enumeratum" % pureconfigVersion

  lazy val log4Cats = "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion

  lazy val log4jOverSlf4j = "org.slf4j" % "log4j-over-slf4j" % slf4jVersion
  lazy val jclOverSlf4j   = "org.slf4j" % "jcl-over-slf4j"   % slf4jVersion
  lazy val julOverSlf4j   = "org.slf4j" % "jul-to-slf4j"     % slf4jVersion

  lazy val log4jSlf4jImpl = "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.13.3"

  lazy val logback = "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime

  lazy val typeDB = "com.vaticle.typedb" % "typedb-client" % typeDBVersion

  lazy val jmorphy2    = Seq(
    "company.evo.jmorphy2" % "jmorphy2-core",
    "company.evo.jmorphy2" % "jmorphy2-dicts-ru"
  ).map(_ % jmorphy2Version)

  lazy val uuidCreator = "com.github.f4b6a3" % "uuid-creator" % uuidCreatorVersion

  lazy val scalaCache = "com.github.cb372" %% "scalacache-caffeine" % scalaCacheVersion

  lazy val catsRetry = "com.github.cb372" %% "cats-retry" % catsRetryVersion

  lazy val tsec = "io.github.jmcardon" %% "tsec-jwt-mac" % Versions.tsecVersion

  lazy val apacheHttpClient = "org.apache.httpcomponents" % "httpclient" % apacheHttpClientVersion

  lazy val curcuit = "io.chrisdavenport" %% "circuit" % circuitVersion

  lazy val joda = "joda-time" % "joda-time" % jodaVersion

  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion

  lazy val magnolia = "com.softwaremill.magnolia1_2" %% "magnolia" % magnoliaVersion

  lazy val weaver  = Seq(
    "com.disneystreaming" %% "weaver-cats",
    "com.disneystreaming" %% "weaver-scalacheck"
  ).map(_ % weaverVersion % Test)

  lazy val mockito = "org.mockito" %% "mockito-scala-cats" % mockitoVersion % Test

  lazy val scalaParserCombs = "org.scala-lang.modules" %% "scala-parser-combinators" % scalaParserCombsVersion

  lazy val csvReader = "com.github.tototoshi" %% "scala-csv" % csvReaderVersion

  lazy val toFuLogging    = Seq(
    "tf.tofu" %% "tofu-core-ce3",
    "tf.tofu" %% "tofu-logging"
  ).map(_ % toFuVersion)

  lazy val clickhouseJdbc = ("com.clickhouse" % "clickhouse-jdbc" % clickhouseJdbcVersion).classifier("all")
}
