import java.util

import com.jsuereth.sbtpgp.SbtPgp.autoImport._
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._
import smartrelease.SmartReleasePlugin.ReleaseSteps
import xerial.sbt.Sonatype.SonatypeKeys._
import xerial.sbt.Sonatype._

object BuildSettings {
  val env: util.Map[String, String] = System.getenv()
  val scala212 = "2.12.13"
  val scala213 = "2.13.5"

  lazy val basicSettings = Seq(
    homepage := Some(new URL("https://github.com/mdsol/mauth-jvm-clients")),
    organization := "com.mdsol",
    organizationHomepage := Some(new URL("http://mdsol.com")),
    description := "MAuth clients",
    scalaVersion := scala213,
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("releases"),
    javacOptions ++= Seq("-encoding", "UTF-8"),
    // Avoid issues such as java.lang.IllegalAccessError: tried to access method org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
    // By running tests in a separate JVM
    Test / fork := true,
    scalacOptions ++= Seq(
      // We deprecated many MAuth v1 methods, and we want to the deprecation warnings
      // for existing tests
      "-Wconf:msg=.*(Uns|S)ignedRequest|signRequest|(decrypt|encrypt)Signature|" +
        "generateRequestHeaders|extract(MwsTime|MAuth)Header|" +
        "generateDigestedMessageV1|generateUnencryptedSignature:s"
    ),
    scalacOptions --= {
      if (sys.env.contains("CI"))
        Seq.empty
      else
        Seq("-Xfatal-warnings")
    },
    credentials += Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", env.get("SONATYPE_USER"), env.get("SONATYPE_TOKEN")),
    publishTo := Some(
      if (isSnapshot.value) {
        Opts.resolver.sonatypeSnapshots
      } else {
        Opts.resolver.sonatypeStaging
      }
    )
  )

  lazy val noPublishSettings = Seq(
    publish / skip := true
  )

  lazy val publishSettings = Seq(
    sonatypeProfileName := "com.mdsol",
    publishMavenStyle := true,
    licenses := Seq("MDSOL" -> url("https://github.com/mdsol/mauth-jvm-clients/blob/master/LICENSE.txt")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/mdsol/mauth-jvm-clients"),
        "scm:git@github.com:mdsol/mauth-jvm-clients.git"
      )
    ),
    developers := List(
      Developer(id = "austek", name = "Ali Ustek", email = "austek@mdsol.com", url = url("https://github.com/austek"))
    ),
    sonatypeProjectHosting := Some(GitHubHosting("austek", "mauth-jvm-clients", "austek@mdsol.com")),
    publishTo := sonatypePublishToBundle.value,
    releaseTagComment := s"Releasing ${(version in ThisBuild).value} [ci skip]",
    releaseCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]",
    releaseNextCommitMessage := s"Setting version to ${(version in ThisBuild).value} [ci skip]",
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseCrossBuild := false, // true if you cross-build the project for multiple Scala versions
    releaseProcess := releaseSteps,
    credentials += Credentials(
      "GnuPG Key ID",
      "pgp",
      "A9A6453ABA90E61B2492BDCD9F58C26F3772CEEE",
      "ignored"
    )
  )

  val releaseSteps: Seq[ReleaseStep] = {
    import sbtrelease.ReleaseStateTransformations._
    Seq(
      checkSnapshotDependencies,
      ReleaseSteps.checkCurrentVersionIsValidReleaseVersion,
      ReleaseSteps.checkReleaseVersionStep,
      ReleaseSteps.fetchAllFromOrigin,
      ReleaseSteps.checkThisCommitExistOnMaster,
      runClean,
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepCommand("sonatypeBundleRelease")
    )
  }
}
