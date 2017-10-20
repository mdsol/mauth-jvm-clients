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
      Dependencies.compile(commonsCodec, commonsLang3, bouncycastlePkix, slf4jApi, typesafeConfig).map(withExclusions) ++
    Dependencies.test(hamcrestAll, jUnitInterface).map(withExclusions)
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
      Dependencies.test(commonsIO, hamcrestAll, jUnitInterface, mockito).map(withExclusions)
  )

lazy val signerApache = (project in file("modules/mauth-signer-apachehttp"))
  .dependsOn(signer, testUtils % "test")
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-signer-apachehttp",
    libraryDependencies ++=
      Dependencies.test(commonsIO, jUnitInterface, mockito).map(withExclusions) ++
        Dependencies.compile(appacheHttpClient).map(withExclusions)
  )

lazy val authenticator = (project in file("modules/mauth-authenticator"))
  .dependsOn(common, testUtils % "test")
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-authenticator",
    libraryDependencies ++=
      Dependencies.compile().map(withExclusions) ++
        Dependencies.test(hamcrestAll, jUnitInterface, mockito).map(withExclusions)
  )

lazy val authenticatorApache = (project in file("modules/mauth-authenticator-apachehttp"))
  .dependsOn(authenticator, signerApache, testUtils % "test")
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-authenticator-apachehttp",
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, guava, slf4jApi).map(withExclusions) ++
        Dependencies.test(hamcrestAll, jUnitInterface, mockito, wiremock).map(withExclusions)
  )

lazy val proxy = (project in file("modules/mauth-proxy"))
  .dependsOn(signerApache)
  .settings(
    basicSettings,
    crossPaths := false,
    name := "mauth-proxy",
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, littleProxy, logbackClassic, logbackCore).map(withExclusions) ++
        Dependencies.test(hamcrestAll, jUnitInterface, wiremock).map(withExclusions)
  )

lazy val mauthClients = (project in file("."))
  .aggregate(authenticator, authenticatorApache, common, proxy, signer, signerApache, testUtils)
  .settings(
    basicSettings,
    publishArtifact := false
  )
