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
        Dependencies.compile(scalaLogging).map(withExclusions) ++
        Dependencies.example(akkaHttp, akkaStream).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock).map(withExclusions)
  )

lazy val `mauth-signer-sttp` = scalaModuleProject("mauth-signer-sttp")
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.compile(scalaLibCompat, sttp, scalaLogging).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock, sttpAkkaHttpBackend).map(withExclusions),
    // TODO remove once published
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
        Dependencies.test(scalaMock, scalaTest, wiremock, sttpAkkaHttpBackend).map(withExclusions),
    // TODO remove once published
    mimaPreviousArtifacts := Set.empty
  )

lazy val `mauth-authenticator` = javaModuleProject("mauth-authenticator")
  .dependsOn(`mauth-common`)
  .settings(
    publishSettings
  )

lazy val `mauth-authenticator-scala` = scalaModuleProject("mauth-authenticator-scala")
  .dependsOn(`mauth-authenticator`, `mauth-test-utils` % "test")
  .settings(
    publishSettings,
    libraryDependencies ++=
      Dependencies.test(logbackClassic, scalaMock, scalaTest).map(withExclusions)
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

lazy val `mauth-proxy` = scalaModuleProject("mauth-proxy")
  .enablePlugins(DockerPlugin)
  .enablePlugins(EcrPlugin)
  .dependsOn(`mauth-signer-apachehttp`)
  .settings(
    publishSettings,
    assemblySettings,
    mimaPreviousArtifacts := Set(),
    // apachehttp uses Guava 27, but littleproxy is compiled against Guava 20 calling now-removed method,
    // throwing java.lang.NoSuchMethodError. Overriding the dep to use v20 seems to work...for now...
    // (This is fine - mauth-proxy is only a helper utility devs/testers run locally)
    dependencyOverrides += "com.google.guava" % "guava" % "20.0",
    crossScalaVersions := Seq(scala213), // This is an application so only need to be published once
    crossPaths := false,
    name := "mauth-proxy",
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, littleProxy, logbackClassic, logbackCore).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock).map(withExclusions),
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"

      new Dockerfile {
        from("java")
        add(artifact, artifactTargetPath)
        entryPoint("java", "-jar", artifactTargetPath)
      }
    },
    region in Ecr := Region.getRegion(Regions.US_EAST_1),
    repositoryName in Ecr := s"mdsol/${name.value.replaceAll("-", "_")}",
    localDockerImage in Ecr := s"${(repositoryName in Ecr).value}:local",
    imageNames in docker := Seq(ImageName((localDockerImage in Ecr).value)),
    repositoryTags in Ecr := {
      if (mainBranch.value) {
        Seq("latest", version.value)
      } else {
        Seq(currentBranch.value)
      }
    },
    push in Ecr := ((push in Ecr) dependsOn (createRepository in Ecr, login in Ecr, DockerKeys.docker)).value,
    buildOptions in docker := BuildOptions(cache = false)
  )

lazy val `mauth-jvm-clients` = (project in file("."))
  .aggregate(
    `mauth-authenticator`,
    `mauth-authenticator-scala`,
    `mauth-authenticator-akka-http`,
    `mauth-authenticator-apachehttp`,
    `mauth-common`,
    `mauth-proxy`,
    `mauth-signer`,
    `mauth-signer-akka-http`,
    `mauth-signer-sttp`,
    `mauth-signer-apachehttp`,
    `mauth-sender-sttp-akka-http`,
    `mauth-test-utils`
  )
  .settings(
    basicSettings,
    publishSettings,
    publish / skip := false,
    smartReleaseAggregateProjectSettings
  )
