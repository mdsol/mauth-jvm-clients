import BuildSettings._
import Dependencies._
import ExampleTesting._
import com.amazonaws.regions.{Region, Regions}

conflictManager := ConflictManager.strict

val withExclusions: ModuleID => ModuleID = moduleId => moduleId.excludeAll(Dependencies.exclusions: _*)

def javaModuleProject(modName: String): Project = {
  Project(modName, file(s"modules/$modName"))
    .settings(
      basicSettings,
      moduleName := modName,
      crossScalaVersions := Seq(scala213),
      crossPaths := false,
      autoScalaLibrary := false
    )
}

def scalaModuleProject(modName: String): Project = {
  Project(modName, file(s"modules/$modName"))
    .settings(
      basicSettings,
      moduleName := modName,
      crossScalaVersions := Seq(scala212, scala213)
    )
}

val currentBranch = Def.setting {
  git.gitCurrentBranch.value.replaceAll("/", "_")
}
val mainBranch = Def.setting {
  currentBranch.value match {
    case _ @("develop" | "master") => true
    case _                         => false
  }
}

lazy val `mauth-common` = javaModuleProject("mauth-common")
  .dependsOn(`mauth-test-utils` % "test")
  .settings(
    publishSettings,
    dependencyOverrides += "commons-codec" % "commons-codec" % "1.13",
    libraryDependencies ++=
      Dependencies.compile(commonsCodec, commonsLang3, bouncyCastlePkix, slf4jApi, typeSafeConfig).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest).map(withExclusions)
  )

lazy val `mauth-test-utils` = javaModuleProject("mauth-test-utils")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(commonsIO, logbackClassic, wiremock).map(withExclusions)
  )

lazy val `mauth-signer` = javaModuleProject("mauth-signer")
  .dependsOn(`mauth-common`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.test(scalaMock, scalaTest).map(withExclusions)
  )

lazy val `mauth-signer-apachehttp` = javaModuleProject("mauth-signer-apachehttp")
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .configs(ExampleTests)
  .settings(
    exampleSettings,
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(apacheHttpClient).map(withExclusions) ++
        Dependencies.compile(caffeine).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest).map(withExclusions)
  )

lazy val `mauth-signer-scala-core` = scalaModuleProject("mauth-signer-scala-core")
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(akkaHttp, akkaStream).map(withExclusions) ++
        Dependencies.compile(scalaLogging, scalaLibCompat).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock).map(withExclusions)
  )

lazy val `mauth-signer-akka-http` = scalaModuleProject("mauth-signer-akka-http")
  .dependsOn(`mauth-signer`, `mauth-signer-scala-core`, `mauth-test-utils` % "test")
  .configs(ExampleTests)
  .settings(
    exampleSettings,
    publishSettings,
    libraryDependencies ++=
      Dependencies.provided(akkaHttp, akkaStream).map(withExclusions) ++
        Dependencies.compile(scalaLogging, scalaLibCompat).map(withExclusions) ++
        Dependencies.example(akkaHttp, akkaStream).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock).map(withExclusions)
  )

lazy val `mauth-signer-sttp` = scalaModuleProject("mauth-signer-sttp")
  .dependsOn(`mauth-signer`, `mauth-signer-scala-core`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(scalaLibCompat, sttp, scalaLogging).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock, sttpAkkaHttpBackend).map(withExclusions)
  )

lazy val `mauth-signer-http4s-023` = scalaModuleProject("mauth-signer-http4s-023")
  .dependsOn(`mauth-signer`, `mauth-signer-scala-core`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++=
      Dependencies.provided(http4sClient) ++
        Dependencies.compile(enumeratum) ++
        Dependencies.compile(log4cats) ++
        Dependencies.test(munitCatsEffect, http4sDsl)
  )

lazy val `mauth-signer-http4s-022` = scalaModuleProject("mauth-signer-http4s-022")
  .dependsOn(`mauth-signer`, `mauth-signer-scala-core`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++=
      Dependencies.provided(http4sClient022) ++
        Dependencies.compile(enumeratum) ++
        Dependencies.test(munitCatsEffect2, http4sDsl022)
  )

// A separate module to sign and send sttp request using akka-http backend
// This keeps mauth-signer-sttp free of dependencies like akka and cats-effect in turn helps reduce dependency footprint
// of our client libraries (which will only need to depend on mauth-signer-sttp)
lazy val `mauth-sender-sttp-akka-http` = scalaModuleProject("mauth-sender-sttp-akka-http")
  .dependsOn(`mauth-signer-sttp`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(catsEffect, akkaHttp, akkaStream, scalaLibCompat, sttp, scalaLogging).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock, sttpAkkaHttpBackend).map(withExclusions)
  )

lazy val `mauth-sender-sttp-http4s-http` = scalaModuleProject("mauth-sender-sttp-http4s-http")
  .dependsOn(`mauth-signer-sttp`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(catsEffect, scalaLibCompat, sttp, sttpFs2, scalaLogging).map(withExclusions) ++
        Dependencies.test(scalaMock, http4sEmberClient, scalaTest, wiremock, sttpHttp4sHttpBackend).map(withExclusions)
  )

lazy val `mauth-authenticator` = javaModuleProject("mauth-authenticator")
  .dependsOn(`mauth-common`)
  .settings(
    publishSettings
  )

lazy val `mauth-authenticator-scala` = scalaModuleProject("mauth-authenticator-scala")
  .dependsOn(`mauth-authenticator`, `mauth-signer` % "test", `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.test(logbackClassic, scalaMock, scalaTest, scalaLibCompat).map(withExclusions) ++
        Dependencies.compile(catsEffect).map(withExclusions)
  )

lazy val `mauth-authenticator-apachehttp` = javaModuleProject("mauth-authenticator-apachehttp")
  .dependsOn(`mauth-authenticator`, `mauth-signer-apachehttp`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, guava, slf4jApi).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock).map(withExclusions)
  )

lazy val `mauth-authenticator-akka-http` = scalaModuleProject("mauth-authenticator-akka-http")
  .dependsOn(`mauth-authenticator-scala` % "test->test;compile->compile", `mauth-signer-akka-http`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.provided(akkaHttp, akkaStream, akkaHttpCache) ++
        Dependencies.compile(jacksonDataBind).map(withExclusions) ++
        Dependencies.test(scalaTest, scalaMock, wiremock) ++
        Dependencies.test(akkaHttpTestKit *).map(withExclusions)
  )

lazy val `mauth-authenticator-http4s` = (project in file("modules/mauth-authenticator-http4s")) // don't need to cross-compile
  .dependsOn(`mauth-signer-http4s-023`, `mauth-authenticator-scala` % "test->test;compile->compile", `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    moduleName := "mauth-authenticator-http4s",
    publishSettings,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++=
      Dependencies.provided(http4sDsl) ++
        Dependencies.provided(http4sClient) ++
        Dependencies.compile(enumeratum) ++
        Dependencies.compile(log4cats) ++
        Dependencies.compile(circeBasic *) ++
        Dependencies.compile(jacksonDataBind, scalaCacheCaffeine) ++
        Dependencies.test(munitCatsEffect) ++
        Dependencies.test(scalaCacheCaffeine) ++
        Dependencies.test(log4catsNoop)
  )

lazy val `mauth-jvm-clients` = (project in file("."))
  .aggregate(
    `mauth-authenticator`,
    `mauth-authenticator-scala`,
    `mauth-authenticator-akka-http`,
    `mauth-authenticator-apachehttp`,
    `mauth-common`,
    `mauth-signer`,
    `mauth-signer-akka-http`,
    `mauth-signer-scala-core`,
    `mauth-signer-http4s-023`,
    `mauth-signer-http4s-022`,
    `mauth-signer-sttp`,
    `mauth-signer-apachehttp`,
    `mauth-sender-sttp-akka-http`,
    `mauth-sender-sttp-http4s-http`,
    `mauth-test-utils`,
    `mauth-authenticator-http4s`
  )
  .settings(
    basicSettings,
    publishSettings,
    publish / skip := false
  )
