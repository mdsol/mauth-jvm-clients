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
    noPublishSettings,
    libraryDependencies ++=
      Dependencies.compile(commonsIO, logbackClassic, wiremock).map(withExclusions)
  )

lazy val `mauth-signer` = scalaModuleProject("mauth-signer")
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

lazy val `mauth-signer-akka-http` = scalaModuleProject("mauth-signer-akka-http")
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
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
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(scalaLibCompat, sttp, scalaLogging).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock, sttpAkkaHttpBackend).map(withExclusions)
  )

lazy val `mauth-signer-http4s` = scalaModuleProject("mauth-signer-http4s")
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    moduleName := "mauth-authenticator-http4s",
    publishSettings,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++=
      Dependencies.provided(http4sClient) ++
        Dependencies.compile(enumeratum) ++
        Dependencies.compile(log4cats) ++
        Dependencies.test(munitCatsEffect, http4sDsl),
    mimaPreviousArtifacts := Set.empty
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
  .dependsOn(`mauth-authenticator-scala`, `mauth-signer-akka-http`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.provided(akkaHttp, akkaStream) ++
        Dependencies.compile(jacksonDataBind, scalaCache).map(withExclusions) ++
        Dependencies.test(scalaTest, scalaMock, wiremock) ++ Dependencies.test(akkaHttpTestKit: _*).map(withExclusions)
  )

lazy val `mauth-authenticator-http4s` = (project in file("modules/mauth-authenticator-http4s")) // don't need to cross-compile
  .dependsOn(`mauth-authenticator-scala`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    moduleName := "mauth-authenticator-http4s",
    publishSettings,
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencies ++=
      Dependencies.provided(http4sDsl) ++
        Dependencies.compile(enumeratum) ++
        Dependencies.compile(log4cats) ++
        Dependencies.test(munitCatsEffect)
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
    `mauth-signer-http4s`,
    `mauth-signer-sttp`,
    `mauth-signer-apachehttp`,
    `mauth-sender-sttp-akka-http`,
    `mauth-test-utils`,
    `mauth-authenticator-http4s`
  )
  .settings(
    basicSettings,
    publishSettings,
    publish / skip := false,
    smartReleaseAggregateProjectSettings
  )
