import java.util

import sbt.Keys._
import sbt._

object BuildSettings {
  val env: util.Map[String, String] = System.getenv()
  val artifactoryUser: String = env.get("ARTIFACTORY_USER")
  val artifactoryToken: String = env.get("ARTIFACTORY_TOKEN")

  lazy val basicSettings = Seq(
    homepage := Some(new URL("https://github.com/mdsol/mauth-java-client")),
    organization := "mdsol",
    organizationHomepage := Some(new URL("http://mdsol.com")),
    description := "MAuth clients",
    scalaVersion := "2.11.8",
    resolvers += Resolver.mavenLocal,
    resolvers ++= Dependencies.resolutionRepos,
    javacOptions ++= Seq("-target", "1.8", "-encoding", "UTF-8"),
    scalacOptions := Seq("-encoding", "UTF-8", "-feature", "-unchecked", "-deprecation", "-language:_", "-Xlog-reflective-calls", "-Ywarn-adapted-args"),
    credentials += Credentials("Artifactory Realm", "artv4.imedidata.net", artifactoryUser, artifactoryToken),
    publishTo := {
      val repoUrl = "https://artv4.imedidata.net/artifactory/"
      if (isSnapshot.value)
        Some("Artifactory snapshots" at repoUrl + "ctms-snapshots;build.timestamp=" + new java.util.Date().getTime)
      else
        Some("Artifactory releases" at repoUrl + "ctms-releases/")
    },
    publishMavenStyle := true
  )
}