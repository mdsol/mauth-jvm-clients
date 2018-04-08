import BuildSettings._
import Dependencies._
import com.amazonaws.regions.{Region, Regions}

conflictManager := ConflictManager.strict
useGpg := false
usePgpKeyHex("87558ab01f3201fc")
pgpPublicRing := baseDirectory.value / "project" / ".gnupg" / "pubring.asc"
pgpSecretRing := baseDirectory.value / "project" / ".gnupg" / "secring.asc"
pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray)

val withExclusions: (ModuleID) => ModuleID = moduleId => moduleId.excludeAll(Dependencies.exclusions: _*)

val currentBranch = Def.setting {
  git.gitCurrentBranch.value.replaceAll("/", "_")
}
val mainBranch = Def.setting {
  currentBranch.value match {
    case _@("develop" | "master") => true
    case _ => false
  }
}

lazy val `mauth-common` = (project in file("modules/mauth-common"))
  .settings(
    basicSettings,
    publishSettings,
    crossPaths := false,
    name := "mauth-common",
    libraryDependencies ++=
      Dependencies.compile(commonsCodec, commonsLang3, bouncyCastlePkix, slf4jApi, typeSafeConfig).map(withExclusions) ++
        Dependencies.test(hamcrestAll, junit, jUnitInterface).map(withExclusions)
  )

lazy val `mauth-test-utils` = (project in file("modules/mauth-test-utils"))
  .settings(
    basicSettings,
    publishSettings,
    crossPaths := false,
    name := "mauth-test-utils",
    libraryDependencies ++=
      Dependencies.compile(commonsIO, wiremock).map(withExclusions)
  )

lazy val `mauth-signer` = (project in file("modules/mauth-signer"))
  .dependsOn(`mauth-common`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    crossPaths := false,
    name := "mauth-signer",
    libraryDependencies ++=
      Dependencies.test(commonsIO, hamcrestAll, junit, jUnitInterface, mockito).map(withExclusions)
  )

lazy val `mauth-signer-apachehttp` = (project in file("modules/mauth-signer-apachehttp"))
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    crossPaths := false,
    name := "mauth-signer-apachehttp",
    libraryDependencies ++=
      Dependencies.test(commonsIO, junit, jUnitInterface, mockito).map(withExclusions) ++
        Dependencies.compile(apacheHttpClient).map(withExclusions)
  )

lazy val `mauth-signer-akka-http` = (project in file("modules/mauth-signer-akka-http"))
  .dependsOn(`mauth-signer`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    name := "mauth-signer-akka-http",
    libraryDependencies ++=
      Dependencies.provided(akkaHttp, akkaStream).map(withExclusions) ++
        Dependencies.compile(scalaLogging, zipkinBrave).map(withExclusions) ++
        Dependencies.test(scalaMock, wiremock).map(withExclusions)
  )

lazy val `mauth-authenticator` = (project in file("modules/mauth-authenticator"))
  .dependsOn(`mauth-common`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    crossPaths := false,
    name := "mauth-authenticator",
    libraryDependencies ++=
      Dependencies.compile().map(withExclusions) ++
        Dependencies.test(hamcrestAll, junit, jUnitInterface, mockito, scalaMock).map(withExclusions)
  )

lazy val `mauth-authenticator-apachehttp` = (project in file("modules/mauth-authenticator-apachehttp"))
  .dependsOn(`mauth-authenticator`, `mauth-signer-apachehttp`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    crossPaths := false,
    name := "mauth-authenticator-apachehttp",
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, guava, slf4jApi).map(withExclusions) ++
        Dependencies.test(hamcrestAll, junit, jUnitInterface, mockito, wiremock).map(withExclusions)
  )

lazy val `mauth-authenticator-akka-http` = (project in file("modules/mauth-authenticator-akka-http"))
  .dependsOn(`mauth-authenticator`, `mauth-signer-akka-http`, `mauth-test-utils` % "test")
  .settings(
    basicSettings,
    publishSettings,
    name := "mauth-authenticator-akka-http",
    libraryDependencies ++=
      Dependencies.provided(akkaHttp, akkaStream) ++
        Dependencies.compile(jacksonDataBind, scalaCache).map(withExclusions) ++
        Dependencies.test(akkaHttpTestKit, hamcrestAll, scalaMock, wiremock).map(withExclusions)
  )

lazy val `mauth-proxy` = (project in file("modules/mauth-proxy"))
  .enablePlugins(DockerPlugin)
  .enablePlugins(EcrPlugin)
  .dependsOn(`mauth-signer-apachehttp`)
  .settings(
    basicSettings,
    publishSettings,
    assemblySettings,
    crossPaths := false,
    name := "mauth-proxy",
    libraryDependencies ++=
      Dependencies.compile(jacksonDataBind, littleProxy, logbackClassic, logbackCore).map(withExclusions) ++
        Dependencies.test(hamcrestAll, junit, jUnitInterface, wiremock).map(withExclusions),
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
    push in Ecr := ((push in Ecr) dependsOn(createRepository in Ecr, login in Ecr, DockerKeys.docker)).value,
    buildOptions in docker := BuildOptions(cache = false)
  )

lazy val `mauth-java-client` = (project in file("."))
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
    publishArtifact := false
  )
