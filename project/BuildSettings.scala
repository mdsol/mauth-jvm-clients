import java.util

import sbt.Keys.*
import sbt.*

object BuildSettings {
  val env: util.Map[String, String] = System.getenv()
  val scala212 = "2.12.20"
  val scala213 = "2.13.18"
  val scala3 = "3.3.7"

  lazy val basicSettings = Seq(
    homepage := Some(new URI("https://github.com/mdsol/mauth-jvm-clients").toURL),
    organization := "com.mdsol",
    organizationHomepage := Some(new URI("http://mdsol.com").toURL),
    description := "MAuth clients",
    scalaVersion := scala213,
    resolvers += Resolver.mavenLocal,
    resolvers ++= Resolver.sonatypeOssRepos("releases"),
    javacOptions ++= Seq("-encoding", "UTF-8"),
    // Avoid issues such as java.lang.IllegalAccessError: tried to access method org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey
    // By running tests in a separate JVM
    Test / fork := true,
    Compile / scalacOptions ++=
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 =>
          List(
            // We deprecated many MAuth v1 methods, and we want to suppress the deprecation warnings
            // for existing tests
            "-Wconf:msg=.*(Uns|S)ignedRequest|signRequest|(decrypt|encrypt)Signature|" +
              "generateRequestHeaders|extract(MwsTime|MAuth)Header|" +
              "generateDigestedMessageV1|generateUnencryptedSignature:s",
            "-Wconf:msg=type Seq in package scala has changed semantics:s",
            "-Wconf:msg=type IndexedSeq in package scala has changed semantics:s",
            "-Wconf:msg=constructor modifiers are assumed by synthetic:s",
            "-Wconf:msg=access modifiers for `copy` method are copied from the case class constructor under:s",
            "-Wconf:msg=access modifiers for `apply` method are copied from the case class constructor under:s",
            "-Wconf:msg=which is not part of the implicit scope in Scala 3:s",
            "-Wconf:msg=Synthetic case companion used as a function.:s",
            "-Wconf:cat=deprecation:s",
            "-Wconf:origin=scala\\.collection\\.compat\\..*&cat=unused:s",
            "-Wnonunit-statement"
          )
        case Some((2, _)) =>
          List()
        case Some((3, _)) =>
          List(
            "-source:3.3",
            "-Wunused:all"
          )
        case _ =>
          List()
      }),
    Test / scalacOptions ++=
      (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 =>
          List(
            "-Xlint:unused",
            "-Wconf:msg=unused value of type.*:s",
            "-Wconf:cat=unused:s",
            "-Wnonunit-statement"
          )
        case Some((2, _)) =>
          List()
        case Some((3, _)) =>
          List(
            // Note: -Wunused:all is already added in Compile / scalacOptions for Scala 3;
            // do NOT repeat it here or Scala 3 will warn about "redundant setting" which
            // becomes a fatal error under -Werror / -Xfatal-warnings
            "-Wconf:msg=unused value of type.*:s",
            // Suppress deprecation warnings from legacy MAuth v1 API usage in tests
            "-Wconf:msg=.*is deprecated.*:s"
          )
        case _ =>
          List()
      }),
    scalacOptions --= {
      if (sys.env.contains("CI"))
        Seq.empty
      else
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, _)) => Seq("-Xfatal-warnings", "-Xlint:unused")
          case _            => Seq("-Xfatal-warnings", "-Wunused:all")
        }
    }
  )

  lazy val publishSettings = Seq(
    licenses := Seq("MDSOL" -> url("https://github.com/mdsol/mauth-jvm-clients/blob/master/LICENSE.txt")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/mdsol/mauth-jvm-clients"),
        "scm:git@github.com:mdsol/mauth-jvm-clients.git"
      )
    ),
    developers := List(
      Developer(
        "scala-mdsol",
        "Medidata Scala Team",
        "list_custom_Medidata.shared-jvm-libs@3ds.com",
        url("https://github.com/mdsol/sbt-smartrelease")
      )
    )
  )
}
