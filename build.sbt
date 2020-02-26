import BuildSettings._
import Dependencies._
import ExampleTesting._
import com.amazonaws.regions.{Region, Regions}
import com.typesafe.tools.mima.core.{DirectMissingMethodProblem, MissingClassProblem, ProblemFilters, ReversedMissingMethodProblem}

conflictManager := ConflictManager.strict

val withExclusions: (ModuleID) => ModuleID = moduleId => moduleId.excludeAll(Dependencies.exclusions: _*)

val javaProjectSettings = Seq(
  crossScalaVersions := Seq(scala213),
  crossPaths := false
)

val scalaProjectSettings = Seq(
  crossScalaVersions := Seq(scala212, scala213)
)

val currentBranch = Def.setting {
  git.gitCurrentBranch.value.replaceAll("/", "_")
}
val mainBranch = Def.setting {
  currentBranch.value match {
    case _ @("develop" | "master") => true
    case _ => false
  }
}

lazy val `mauth-common` = (project in file("modules/mauth-common"))
  .dependsOn(`mauth-test-utils` % "test")
  .settings(
    basicSettings,
    javaProjectSettings,
    publishSettings,
    name := "mauth-common",
    dependencyOverrides += "commons-codec" % "commons-codec" % "1.13",
    libraryDependencies ++=
      Dependencies.compile(commonsCodec, commonsLang3, bouncyCastlePkix, slf4jApi, typeSafeConfig).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest).map(withExclusions)
  )

lazy val `mauth-test-utils` = (project in file("modules/mauth-test-utils"))
  .settings(
    basicSettings,
    publishSettings,
    javaProjectSettings,
    name := "mauth-test-utils",
    libraryDependencies ++=
      Dependencies.compile(commonsIO, logbackClassic, wiremock).map(withExclusions)
  )

lazy val `mauth-signer` = (project in file("modules/mauth-signer"))
  .dependsOn(`mauth-common`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    javaProjectSettings,
    name := "mauth-signer",
    libraryDependencies ++=
      Dependencies.test(scalaMock, scalaTest).map(withExclusions),
    mimaBinaryIssueFilters ++= Seq(
      // TODO: Remove after Mauth v2 is released
      ProblemFilters.exclude[ReversedMissingMethodProblem]("com.mdsol.mauth.Signer.generateRequestHeaders")
    )
  )

lazy val `mauth-signer-apachehttp` = (project in file("modules/mauth-signer-apachehttp"))
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .configs(ExampleTests)
  .settings(
    basicSettings,
    exampleSettings,
    publishSettings,
    javaProjectSettings,
    name := "mauth-signer-apachehttp",
    libraryDependencies ++=
      Dependencies.compile(apacheHttpClient).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest).map(withExclusions)
  )

lazy val `mauth-signer-akka-http` = (project in file("modules/mauth-signer-akka-http"))
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .configs(ExampleTests)
  .settings(
    basicSettings,
    exampleSettings,
    publishSettings,
    scalaProjectSettings,
    name := "mauth-signer-akka-http",
    libraryDependencies ++=
      Dependencies.provided(akkaHttp, akkaStream).map(withExclusions) ++
        Dependencies.compile(scalaLogging).map(withExclusions) ++
        Dependencies.example(akkaHttp, akkaStream).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock).map(withExclusions),
    mimaBinaryIssueFilters ++= Seq(
      // TODO: Remove after Mauth v2 is released
      ProblemFilters.exclude[ReversedMissingMethodProblem]("com.mdsol.mauth.RequestSigner.signRequest"),
      ProblemFilters.exclude[MissingClassProblem]("com.mdsol.mauth.http.TraceHttpClient")
    )
  )

lazy val `mauth-authenticator` = (project in file("modules/mauth-authenticator"))
  .dependsOn(`mauth-common`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    javaProjectSettings,
    name := "mauth-authenticator",
    libraryDependencies ++=
      Dependencies.test(logbackClassic, scalaMock, scalaTest).map(withExclusions),
    mimaBinaryIssueFilters ++= Seq(
      // TODO: Remove after Mauth v2 is released
      ProblemFilters.exclude[ReversedMissingMethodProblem]("com.mdsol.mauth.scaladsl.Authenticator.isV2OnlyAuthenticate")
    )
  )

lazy val `mauth-authenticator-apachehttp` = (project in file("modules/mauth-authenticator-apachehttp"))
  .dependsOn(`mauth-authenticator`, `mauth-signer-apachehttp`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    javaProjectSettings,
    name := "mauth-authenticator-apachehttp",
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, guava, slf4jApi).map(withExclusions) ++
        Dependencies.test(scalaMock, scalaTest, wiremock).map(withExclusions)
  )

lazy val `mauth-authenticator-akka-http` = (project in file("modules/mauth-authenticator-akka-http"))
  .dependsOn(`mauth-authenticator`, `mauth-signer-akka-http`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    scalaProjectSettings,
    name := "mauth-authenticator-akka-http",
    libraryDependencies ++=
      Dependencies.provided(akkaHttp, akkaStream) ++
        Dependencies.compile(jacksonDataBind, scalaCache).map(withExclusions) ++
        Dependencies.test(scalaTest, scalaMock, wiremock) ++ Dependencies.test(akkaHttpTestKit: _*).map(withExclusions),
    mimaBinaryIssueFilters ++= Seq(
      // TODO: Remove after Mauth v2 is released
      ProblemFilters.exclude[DirectMissingMethodProblem]("com.mdsol.mauth.akka.http.utils.MAuthSignatureEngine.logger"),
      ProblemFilters.exclude[MissingClassProblem]("com.mdsol.mauth.akka.http.TraceMauthPublicKeyProvider")
    )
  )

lazy val `mauth-proxy` = (project in file("modules/mauth-proxy"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(EcrPlugin)
  .dependsOn(`mauth-signer-apachehttp`)
  .settings(
    basicSettings,
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
    `mauth-authenticator-akka-http`,
    `mauth-authenticator-apachehttp`,
    `mauth-common`,
    `mauth-proxy`,
    `mauth-signer`,
    `mauth-signer-akka-http`,
    `mauth-signer-apachehttp`,
    `mauth-test-utils`
  )
  .settings(
    basicSettings,
    publishSettings,
    publishArtifact := false,
    mimaPreviousArtifacts := Set.empty
  )
