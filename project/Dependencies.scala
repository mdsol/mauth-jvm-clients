import sbt._

object Dependencies {

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")

  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")

  def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")

  def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")

  def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  private object Version {
    val akkaHttp = "10.1.1"
    val logback = "1.2.3"
  }

  val akkaHttp: ModuleID = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val akkaStream: ModuleID = "com.typesafe.akka" %% "akka-stream" % "2.5.11"
  val apacheHttpClient: ModuleID = "org.apache.httpcomponents" % "httpclient" % "4.5.5"
  val bouncyCastlePkix: ModuleID = "org.bouncycastle" % "bcpkix-jdk15on" % "1.59"
  val commonsCodec: ModuleID = "commons-codec" % "commons-codec" % "1.11"
  val commonsLang3: ModuleID = "org.apache.commons" % "commons-lang3" % "3.7"
  val guava: ModuleID = "com.google.guava" % "guava" % "23.0"
  val jacksonDataBind: ModuleID = "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.5"
  val littleProxy: ModuleID = "org.littleshoot" % "littleproxy" % "1.1.2"
  val logbackClassic: ModuleID = "ch.qos.logback" % "logback-classic" % Version.logback
  val logbackCore: ModuleID = "ch.qos.logback" % "logback-core" % Version.logback
  val slf4jApi: ModuleID = "org.slf4j" % "slf4j-api" % "1.7.25"
  val typeSafeConfig: ModuleID = "com.typesafe" % "config" % "1.3.3"
  val scalaCache: ModuleID = "com.github.cb372" %% "scalacache-guava" % "0.23.0"
  val scalaLogging: ModuleID = "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0"
  val zipkinBrave = "io.zipkin.brave" % "brave" % "4.18.2"

  // TEST DEPENDENCIES
  val akkaHttpTestKit: ModuleID = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp
  val commonsIO: ModuleID = "commons-io" % "commons-io" % "2.6"
  val scalaMock: ModuleID = "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0"
  val wiremock: ModuleID = "com.github.tomakehurst" % "wiremock" % "2.16.0"

  // Dependency Conflict Resolution
  val exclusions = Seq()
  val mccLibDependencyOverrides = Set()
}
