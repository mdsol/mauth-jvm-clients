import BuildSettings._
import Dependencies._

conflictManager := ConflictManager.strict
val withExclusions: (ModuleID) => ModuleID = moduleId => moduleId.excludeAll(Dependencies.exclusions: _*)

lazy val common = (project in file("modules/mauth-common"))
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-common",
    libraryDependencies ++=
      Dependencies.compile(commonsCodec, commonsLang3, bouncyCastlePkix, slf4jApi, typeSafeConfig).map(withExclusions) ++
        Dependencies.test(hamcrestAll, junit, jUnitInterface).map(withExclusions)
  )

lazy val testUtils = (project in file("modules/mauth-test-utils"))
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-test-utils",
    libraryDependencies ++=
      Dependencies.compile(commonsIO, wiremock).map(withExclusions)
  )

lazy val signer = (project in file("modules/mauth-signer"))
  .dependsOn(common, testUtils % "test")
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-signer",
    libraryDependencies ++=
      Dependencies.test(commonsIO, hamcrestAll, junit, jUnitInterface, mockito).map(withExclusions)
  )

lazy val signerApache = (project in file("modules/mauth-signer-apachehttp"))
  .dependsOn(signer, testUtils % "test")
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-signer-apachehttp",
    libraryDependencies ++=
      Dependencies.test(commonsIO, junit, jUnitInterface, mockito).map(withExclusions) ++
        Dependencies.compile(apacheHttpClient).map(withExclusions)
  )

lazy val signerAkka = (project in file("modules/mauth-signer-akka-http"))
  .dependsOn(signer, testUtils % "test")
  .settings(
    basicSettings,
    name := "mauth-signer-akka-http",
    libraryDependencies ++=
      Dependencies.provided(akkaHttp).map(withExclusions) ++
        Dependencies.compile(scalaLogging).map(withExclusions) ++
        Dependencies.test(scalaTest).map(withExclusions)
  )

lazy val authenticator = (project in file("modules/mauth-authenticator"))
  .dependsOn(common, testUtils % "test")
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-authenticator",
    libraryDependencies ++=
      Dependencies.compile().map(withExclusions) ++
        Dependencies.test(hamcrestAll, junit, jUnitInterface, mockito, scalaTest).map(withExclusions)
  )

lazy val authenticatorApache = (project in file("modules/mauth-authenticator-apachehttp"))
  .dependsOn(authenticator, signerApache, testUtils % "test")
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-authenticator-apachehttp",
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, guava, slf4jApi).map(withExclusions) ++
        Dependencies.test(hamcrestAll, junit, jUnitInterface, mockito, wiremock).map(withExclusions)
  )

lazy val authenticatorAkka = (project in file("modules/mauth-authenticator-akka-http"))
  .dependsOn(authenticator, signerAkka, testUtils % "test")
  .settings(
    basicSettings,
    name := "mauth-authenticator-akka-http",
    libraryDependencies ++=
      Dependencies.provided(akkaHttp) ++
        Dependencies.compile(jacksonDataBind, scalaCache).map(withExclusions) ++
        Dependencies.test(akkaHttpTestKit, hamcrestAll, mockito, scalaTest, wiremock).map(withExclusions)
  )

lazy val proxy = (project in file("modules/mauth-proxy"))
  .dependsOn(signerApache)
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-proxy",
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, littleProxy, logbackClassic, logbackCore).map(withExclusions) ++
        Dependencies.test(hamcrestAll, junit, jUnitInterface, wiremock).map(withExclusions)
  )

lazy val mauthClients = (project in file("."))
  .aggregate(authenticator, authenticatorAkka, authenticatorApache, common, proxy, signer, signerAkka, signerApache, testUtils)
  .settings(
    basicSettings,
    publishArtifact := false
  )
