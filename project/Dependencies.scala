import sbt._

object Dependencies extends DependencyUtils {

  private object Version {
    val akka = "2.6.13"
    val akkaHttp = "10.2.4"
    val logback = "1.2.3"
    val sttp = "3.2.0"
    val http4s = "0.23.14"
    val enumeratum = "1.7.0"
    val log4cats = "2.1.1"
  }

  val akkaHttp: ModuleID = "com.typesafe.akka"                        %% "akka-http"               % Version.akkaHttp
  val akkaStream: ModuleID = "com.typesafe.akka"                      %% "akka-stream"             % Version.akka
  val apacheHttpClient: ModuleID = "org.apache.httpcomponents"         % "httpclient"              % "4.5.13"
  val bouncyCastlePkix: ModuleID = "org.bouncycastle"                  % "bcpkix-jdk15on"          % "1.68"
  val commonsCodec: ModuleID = "commons-codec"                         % "commons-codec"           % "1.15"
  val commonsLang3: ModuleID = "org.apache.commons"                    % "commons-lang3"           % "3.12.0"
  val guava: ModuleID = "com.google.guava"                             % "guava"                   % "28.0-jre"
  val jacksonDataBind: ModuleID = "com.fasterxml.jackson.core"         % "jackson-databind"        % "2.12.2"
  val logbackClassic: ModuleID = "ch.qos.logback"                      % "logback-classic"         % Version.logback
  val logbackCore: ModuleID = "ch.qos.logback"                         % "logback-core"            % Version.logback
  val slf4jApi: ModuleID = "org.slf4j"                                 % "slf4j-api"               % "1.7.30"
  val typeSafeConfig: ModuleID = "com.typesafe"                        % "config"                  % "1.4.1"
  val scalaCache: ModuleID = "com.github.cb372"                       %% "scalacache-caffeine"     % "1.0.0-M6"
  val scalaLogging: ModuleID = "com.typesafe.scala-logging"           %% "scala-logging"           % "3.9.3"
  val catsEffect: ModuleID = "org.typelevel"                          %% "cats-effect"             % "3.2.9"
  val sttp: ModuleID = "com.softwaremill.sttp.client3"                %% "core"                    % Version.sttp
  val sttpAkkaHttpBackend: ModuleID = "com.softwaremill.sttp.client3" %% "akka-http-backend"       % Version.sttp
  val scalaLibCompat: ModuleID = "org.scala-lang.modules"             %% "scala-collection-compat" % "2.4.3"
  val caffeine: ModuleID = "com.github.ben-manes.caffeine"             % "caffeine"                % "3.1.1"
  val http4sDsl: ModuleID = "org.http4s"                              %% "http4s-dsl"              % Version.http4s
  val http4sClient: ModuleID = "org.http4s"                           %% "http4s-client"           % Version.http4s
  val enumeratum: ModuleID = "com.beachape"                           %% "enumeratum"              % Version.enumeratum
  val log4cats = "org.typelevel"                                      %% "log4cats-slf4j"          % Version.log4cats

  // TEST DEPENDENCIES
  val akkaHttpTestKit: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http-testkit"   % Version.akkaHttp,
    "com.typesafe.akka" %% "akka-testkit"        % Version.akka,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
  )
  val commonsIO: ModuleID = "commons-io"            % "commons-io"          % "2.8.0"
  val scalaMock: ModuleID = "org.scalamock"        %% "scalamock"           % "5.1.0"
  val scalaTest: ModuleID = "org.scalatest"        %% "scalatest"           % "3.2.7"
  val wiremock: ModuleID = "com.github.tomakehurst" % "wiremock"            % "2.27.2"
  val munitCatsEffect: ModuleID = "org.typelevel"  %% "munit-cats-effect-3" % "1.0.7"

  // Dependency Conflict Resolution
  val exclusions = Seq()
  val mccLibDependencyOverrides = Set()
}
