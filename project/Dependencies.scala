import sbt._

object Dependencies {

  val resolutionRepos = Seq(
    "Artifactory" at "https://artv4.imedidata.net/artifactory/all-repos-release"
  )

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")

  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")

  def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")

  def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")

  def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  private object Version {
    val akkaHttp = "10.0.10"
    val logback = "1.2.3"
  }

  val akkaHttp: ModuleID = "com.typesafe.akka" %% "akka-http" % Version.akkaHttp
  val apacheHttpClient: ModuleID = "org.apache.httpcomponents" % "httpclient" % "4.5.3"
  val bouncyCastlePkix: ModuleID = "org.bouncycastle" % "bcpkix-jdk15on" % "1.57"
  val commonsCodec: ModuleID = "commons-codec" % "commons-codec" % "1.10"
  val commonsLang3: ModuleID = "org.apache.commons" % "commons-lang3" % "3.6"
  val guava: ModuleID = "com.google.guava" % "guava" % "23.0"
  val jacksonDataBind: ModuleID = "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.2"
  val littleProxy: ModuleID = "org.littleshoot" % "littleproxy" % "1.1.2"
  val logbackClassic: ModuleID = "ch.qos.logback" % "logback-classic" % Version.logback
  val logbackCore: ModuleID = "ch.qos.logback" % "logback-core" % Version.logback
  val slf4jApi: ModuleID = "org.slf4j" % "slf4j-api" % "1.7.25"
  val typeSafeConfig: ModuleID = "com.typesafe" % "config" % "1.3.2"
  val scalaCache: ModuleID = "com.github.cb372" %% "scalacache-guava" % "0.10.0"
  val scalaLogging: ModuleID = "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"

  // TEST DEPENDENCIES
  val akkaHttpTestKit: ModuleID = "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp
  val commonsIO: ModuleID = "commons-io" % "commons-io" % "2.6"
  val hamcrestAll: ModuleID = "org.hamcrest" % "hamcrest-all" % "1.3"
  val junit: ModuleID = "junit" % "junit" % "4.12"
  val jUnitInterface: ModuleID = "com.novocode" % "junit-interface" % "0.11" exclude("junit", "junit")
  val mockito: ModuleID = "org.mockito" % "mockito-all" % "1.10.19"
  val scalaTest: ModuleID = "org.scalatest" %% "scalatest" % "3.0.3"
  val wiremock: ModuleID = "com.github.tomakehurst" % "wiremock" % "1.58"


  // Dependency Conflict Resolution
  val exclusions = Seq()
  val mccLibDependencyOverrides = Set()
}
