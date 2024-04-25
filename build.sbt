ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

ThisBuild / resolvers ++= Dependencies.Resolvers.resolvers

lazy val commonSettings = Seq(
  addCompilerPlugin(Dependencies.kindProjector),
  addCompilerPlugin(Dependencies.betterMonadicFor),
  scalafmtOnCompile := true,
  scalafixOnCompile := true,
  Global / onChangedBuildSource := ReloadOnSourceChanges,
  Global / cancelable := true,
  Test / fork := true,
  turbo := true,
  scalacOptions ++= Seq(
    "-deprecation",               // Emit warning and location for usages of deprecated APIs.
    "-explaintypes",              // Explain type errors in more detail.
    "-feature",                   // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked",                 // Enable additional warnings where generated code depends on assumptions.
    "-Wdead-code",                // Warn when dead code is identified.
    "-Wextra-implicit",           // Warn when more than one implicit parameter section is defined.
    "-Wdead-code",                // Warn when dead code is identified.
    "-Wextra-implicit",           // Warn when more than one implicit parameter section is defined.
    "-Wunused",                   // Warn if something from check list is unused.
    "-Wvalue-discard",            // Warn when non-Unit expression results are unused.
    "-Ywarn-macros:after",        // Needed for correct implicit resolution.
    "-Wconf:cat=unused-nowarn:s", // Silence nowarn usage warnings.
    "-Xfatal-warnings"            // Fail the compilation if there are any warnings.
  ),
  scalacOptions -= "-Xfatal-warnings",
  Test / scalacOptions -= "-Wdead-code", // Allow using the any or * matchers in tests
  libraryDependencies += Dependencies.cats,
  libraryDependencies += Dependencies.catsEffect
)

lazy val app = (project in file("text2ql"))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(GitVersioning)
  .dependsOn(webService, domainSchema)
  .settings(commonSettings)
  .settings(
    name := "text2ql-app",
    libraryDependencies += Dependencies.logback,
    libraryDependencies += Dependencies.jclOverSlf4j,
    libraryDependencies += Dependencies.catsRetry
  )
  .settings(
    excludeDependencies ++= Seq(
      // commons-logging is replaced by jcl-over-slf4j
      ExclusionRule("commons-logging", "commons-logging"),
      // log4j is replaced by log4j-over-slf4j
      ExclusionRule("log4j", "log4j"),
      ExclusionRule("org.apache.logging.log4j", "log4j-core"),
      ExclusionRule("org.apache.logging.log4j", "log4j-api")
    )
  )

lazy val webService = project
  .in(file("modules/web-service"))
  .dependsOn(models)
  .settings(commonSettings)
  .settings(
    name := "web-service",
    libraryDependencies += Dependencies.pureconfig,
    libraryDependencies += Dependencies.pureconfigCE,
    libraryDependencies ++= Dependencies.http4s,
    libraryDependencies += Dependencies.http4sCirce,
    libraryDependencies += Dependencies.prometheusTapir,
    libraryDependencies += Dependencies.fs2,
    libraryDependencies += Dependencies.log4Cats,
    libraryDependencies += Dependencies.tapirCore,
    libraryDependencies ++= Dependencies.tapir,
    libraryDependencies += Dependencies.tapirRefined,
    libraryDependencies ++= Dependencies.tapirServer,
    libraryDependencies += Dependencies.tapirSttpClient,
    libraryDependencies += Dependencies.tapirSttpStubServer,
    libraryDependencies += Dependencies.sttpCore,
    libraryDependencies += Dependencies.sttpClient3Core,
    libraryDependencies ++= Dependencies.circe,
    libraryDependencies += Dependencies.circeFs2,
    libraryDependencies += Dependencies.circeEnumeratum,
    libraryDependencies += Dependencies.circeGenericExtras
  )

lazy val models = project
  .in(file("modules/models"))
  .settings(name := "models")
  .settings(commonSettings)
  .settings(
    libraryDependencies += Dependencies.circeEnumeratum,
    libraryDependencies += Dependencies.quillEnumeratum,
    libraryDependencies += Dependencies.circeGenericExtras,
    libraryDependencies ++= Dependencies.tapir,
    libraryDependencies += Dependencies.uuidCreator,
    libraryDependencies += Dependencies.pureconfig,
    libraryDependencies += Dependencies.scalaLogging,
    libraryDependencies += Dependencies.joda,
    libraryDependencies += Dependencies.chimney,
    libraryDependencies ++= Dependencies.http4s,
    libraryDependencies += Dependencies.log4Cats
  )

lazy val postgres = project
  .in(file("modules/postgres"))
  .settings(commonSettings)
  .dependsOn(models)
  .settings(
    name := "postgres",
    libraryDependencies += Dependencies.prometheusTapir,
    libraryDependencies ++= Dependencies.doobie,
    libraryDependencies += Dependencies.liquibase
  )

lazy val domainSchema = project
  .in(file("modules/domain-schema"))
  .settings(name := "domain-schema")
  .settings(commonSettings)
  .settings(scalacOptions += "-Wconf:msg=doobieContext.Quoted:s")
  .settings(libraryDependencies += Dependencies.circeYaml)
  .dependsOn(postgres)
