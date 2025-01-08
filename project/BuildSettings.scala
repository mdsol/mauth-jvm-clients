import java.util

import sbt.Keys._
import sbt._

object BuildSettings {
  val env: util.Map[String, String] = System.getenv()
  val scala212 = "2.12.17"
  val scala213 = "2.13.15"

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
    }
  )

  lazy val noPublishSettings = Seq(
    publish / skip := true
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
