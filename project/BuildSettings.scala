import java.util

import com.typesafe.sbt.pgp.PgpKeys
import sbt.Keys._
import sbt.{url, _}
import sbtassembly.AssemblyKeys._
import sbtassembly.MergeStrategy
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

object BuildSettings {
  val env: util.Map[String, String] = System.getenv()
  val scala211 = "2.11.11"
  val scala212 = "2.12.6"

  lazy val basicSettings = Seq(
    homepage := Some(new URL("https://github.com/mdsol/mauth-java-client")),
    organization := "com.mdsol",
    organizationHomepage := Some(new URL("http://mdsol.com")),
    description := "MAuth clients",
    scalaVersion := scala212,
    crossScalaVersions := Seq(scala211, scala212),
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("releases"),
    javacOptions ++= Seq("-encoding", "UTF-8"),
    // Avoid issues such as java.lang.IllegalAccessError: tried to access method org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
    // By running tests in a separate JVM
    Test / fork := true,
    scalacOptions := Seq(
      "-encoding",
      "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.8",
      "-language:_",
      "-Xlog-reflective-calls",
      "-Ywarn-adapted-args"
    ),
    credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", env.get("SONATYPE_USER"), env.get("SONATYPE_TOKEN")),
    publishTo := Some(
      if (isSnapshot.value) {
        Opts.resolver.sonatypeSnapshots
      } else {
        Opts.resolver.sonatypeStaging
      }
    )
  )

  lazy val publishSettings = Seq(
    mimaPreviousArtifacts := (if (crossPaths.value) { // Scala project
                                Set(organization.value %% name.value % "1.0.16")
                              } else {
                                Set(organization.value % name.value % "1.0.16")
                              }),
    publishMavenStyle := true,
    licenses := Seq("MDSOL" -> url("https://github.com/mdsol/mauth-java-client/blob/master/LICENSE.txt")),
    pomIncludeRepository := { _ =>
      false
    },
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/mdsol/mauth-java-client"),
        "scm:git@github.com:mdsol/mauth-java-client.git"
      )
    ),
    developers := List(
      Developer(id = "austek", name = "Ali Ustek", email = "austek@mdsol.com", url = url("https://github.com/austek"))
    ),
    releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]",
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseVersionBump := sbtrelease.Version.Bump.Bugfix,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      releaseStepCommand("sonatypeReleaseAll"),
      pushChanges
    )
  )

  lazy val assemblySettings = Seq(
    test in assembly := {},
    mainClass in assembly := Some("com.mdsol.mauth.proxy.ProxyServer"),
    assemblyJarName in assembly := s"mauth-proxy-${version.value}.jar",
    assemblyMergeStrategy in assembly := {
      case "logback.xml" => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
}
