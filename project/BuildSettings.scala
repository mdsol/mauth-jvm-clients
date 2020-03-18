import java.util

import com.jsuereth.sbtpgp.SbtPgp.autoImport._
import sbt.Keys._
import sbt.{url, _}
import sbtassembly.AssemblyKeys._
import sbtassembly.{MergeStrategy, PathList}
import sbtrelease.ReleasePlugin.autoImport._
import xerial.sbt.Sonatype._
import xerial.sbt.Sonatype.SonatypeKeys._
import sbtrelease.ReleaseStateTransformations._
import com.typesafe.tools.mima.plugin.MimaPlugin.autoImport._

import scala.sys.process.{Process, ProcessLogger}
import scala.util.Try

object BuildSettings {
  val env: util.Map[String, String] = System.getenv()
  val scala212 = "2.12.10"
  val scala213 = "2.13.1"

  lazy val basicSettings = Seq(
    homepage := Some(new URL("https://github.com/mdsol/mauth-jvm-clients")),
    organization := "com.mdsol",
    organizationHomepage := Some(new URL("http://mdsol.com")),
    description := "MAuth clients",
    scalaVersion := scala213,
    resolvers += Resolver.mavenLocal,
    resolvers += Resolver.sonatypeRepo("releases"),
    libraryDependencies ++= Dependencies.silencer,
    javacOptions ++= Seq("-encoding", "UTF-8"),
    // Avoid issues such as java.lang.IllegalAccessError: tried to access method org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
    // By running tests in a separate JVM
    Test / fork := true,
    scalacOptions ++= commonScalacOptions ++ silencerOptions ++ {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => scalacOptionsFor212
        case Some((2, 13)) => scalacOptionsFor213
        case _ => Nil
      }
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

  private lazy val silencerOptions = Seq(
    "-P:silencer:checkUnused",
    "-P:silencer:globalFilters=since 2.13.0;" +
      ".*(Uns|S)ignedRequest.*;" +
      ".*in class (DefaultSigner|MAuthRequestSigner|MAuthSignatureHelper) is deprecated.*;" +
      ".*X_MWS_(AUTHENTICATION|TIME)_HEADER_NAME.*;" +
      ".*extract(MwsTime|MAuth)Header.*",
    "-P:silencer:pathFilters=target/.*"
  )

  private lazy val commonScalacOptions = Seq(
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-language:existentials",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-unchecked",
    "-Xcheckinit",
    "-Xlint:adapted-args",
    "-Xlint:constant",
    "-Xlint:delayedinit-select",
    "-Xlint:doc-detached",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:missing-interpolator",
    "-Xlint:nullary-override",
    "-Xlint:nullary-unit",
    "-Xlint:option-implicit",
    "-Xlint:package-object-classes",
    "-Xlint:poly-implicit-overload",
    "-Xlint:private-shadow",
    "-Xlint:stars-align",
    "-Xlint:type-parameter-shadow"
  )

  // See https://tpolecat.github.io/2017/04/25/scalac-flags.html
  private lazy val scalacOptionsFor212 = Seq(
    "-Xfatal-warnings",
    "-Xfuture",
    "-Xlint:by-name-right-associative",
    "-Xlint:unsound-match",
    "-Yno-adapted-args",
    "-Ypartial-unification",
    "-Ywarn-dead-code",
    "-Ywarn-extra-implicit",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused:implicits",
    "-Ywarn-unused:imports",
    "-Ywarn-unused:locals",
    "-Ywarn-unused:params",
    "-Ywarn-unused:patvars",
    "-Ywarn-unused:privates",
    "-Ywarn-value-discard"
  )

  // See https://nathankleyn.com/2019/05/13/recommended-scalac-flags-for-2-13
  private lazy val scalacOptionsFor213 = Seq(
    "-language:reflectiveCalls",
    "-Wextra-implicit",
    "-Wnumeric-widen",
    "-Woctal-literal",
    "-Wunused:explicits",
    "-Wunused:implicits",
    "-Wunused:imports",
    "-Wunused:locals",
    "-Wunused:patvars",
    "-Wunused:privates",
    "-Wunused:privates",
    "-Wunused:linted",
    "-Wvalue-discard",
    "-Werror"
  )

  lazy val publishSettings = Seq(
    mimaPreviousArtifacts := {
      execAndHandleEmptyOutput(Some(baseDirectory.value), "git describe --tags --abbrev=0 --match v[0-9]*") match {
        case Some(latestTag) =>
          val latestStableReleaseVersion = latestTag.replaceAll("^v", "").trim
          val versionMajor: Option[Long] = VersionNumber(version.value)._1
          val latestVersionMajor: Long = VersionNumber(latestStableReleaseVersion)._1.getOrElse(0)
          if (versionMajor.exists(_ > latestVersionMajor)) {
            Set.empty
          } else if (crossPaths.value) { // Scala project
            Set(organization.value %% name.value % latestStableReleaseVersion)
          } else {
            Set(organization.value % name.value % latestStableReleaseVersion)
          }
        case None => Set.empty
      }
    },
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
    releaseVersionBump := sbtrelease.Version.Bump.Bugfix,
    releaseCrossBuild := false, // true if you cross-build the project for multiple Scala versions
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
    credentials += Credentials(
      "GnuPG Key ID",
      "pgp",
      "A9A6453ABA90E61B2492BDCD9F58C26F3772CEEE",
      "ignored"
    )
  )

  lazy val assemblySettings = Seq(
    test in assembly := {},
    mainClass in assembly := Some("com.mdsol.mauth.proxy.ProxyServer"),
    assemblyJarName in assembly := s"mauth-proxy-${version.value}.jar",
    assemblyMergeStrategy in assembly := {
      case "logback.xml" => MergeStrategy.first
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x if x.endsWith("module-info.class") => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )

  private def execAndHandleEmptyOutput(wd: Option[File], cmd: String): Option[String] =
    Try(Process(cmd, wd) !! NoProcessLogger).toOption
      .filter(_.trim.nonEmpty)
}

object NoProcessLogger extends ProcessLogger {
  def info(s: => String): Unit = ()
  def out(s: => String): Unit = ()
  def error(s: => String): Unit = ()
  def err(s: => String): Unit = ()
  def buffer[T](f: => T): T = f
}
