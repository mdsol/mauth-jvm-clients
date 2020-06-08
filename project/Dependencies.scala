import sbt._

object Dependencies extends DependencyUtils {

  private object Version {
    val akka = "2.6.3"
    val akkaHttp = "10.1.11"
    val logback = "1.2.3"
    val silencer: String = "1.6.0"
    val sttp = "2.1.5"
  }

  val akkaHttp: ModuleID = "com.typesafe.akka"                       %% "akka-http"               % Version.akkaHttp
  val akkaStream: ModuleID = "com.typesafe.akka"                     %% "akka-stream"             % Version.akka
  val apacheHttpClient: ModuleID = "org.apache.httpcomponents"       % "httpclient"               % "4.5.11"
  val bouncyCastlePkix: ModuleID = "org.bouncycastle"                % "bcpkix-jdk15on"           % "1.64"
  val commonsCodec: ModuleID = "commons-codec"                       % "commons-codec"            % "1.14"
  val commonsLang3: ModuleID = "org.apache.commons"                  % "commons-lang3"            % "3.9"
  val guava: ModuleID = "com.google.guava"                           % "guava"                    % "23.0"
  val jacksonDataBind: ModuleID = "com.fasterxml.jackson.core"       % "jackson-databind"         % "2.10.2"
  val littleProxy: ModuleID = "org.littleshoot"                      % "littleproxy"              % "1.1.2"
  val logbackClassic: ModuleID = "ch.qos.logback"                    % "logback-classic"          % Version.logback
  val logbackCore: ModuleID = "ch.qos.logback"                       % "logback-core"             % Version.logback
  val slf4jApi: ModuleID = "org.slf4j"                               % "slf4j-api"                % "1.7.30"
  val typeSafeConfig: ModuleID = "com.typesafe"                      % "config"                   % "1.4.0"
  val scalaCache: ModuleID = "com.github.cb372"                      %% "scalacache-guava"        % "0.28.0"
  val scalaLogging: ModuleID = "com.typesafe.scala-logging"          %% "scala-logging"           % "3.9.2"
  val catsEffect: ModuleID = "org.typelevel"                         %% "cats-effect"             % "2.1.3"
  val sttp: ModuleID = "com.softwaremill.sttp.client"                %% "core"                    % Version.sttp
  val sttpAkkaHttpBackend: ModuleID = "com.softwaremill.sttp.client" %% "akka-http-backend"       % Version.sttp
  val scalaLibCompat: ModuleID = "org.scala-lang.modules"            %% "scala-collection-compat" % "2.1.6"

  // TEST DEPENDENCIES
  // Not sure why they don't make the akka-http test kit depends on other test kits...
  val akkaHttpTestKit: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http-testkit"   % Version.akkaHttp,
    "com.typesafe.akka" %% "akka-testkit"        % Version.akka,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
  )
  val commonsIO: ModuleID = "commons-io"            % "commons-io" % "2.6"
  val scalaMock: ModuleID = "org.scalamock"         %% "scalamock" % "4.4.0"
  val scalaTest: ModuleID = "org.scalatest"         %% "scalatest" % "3.1.1"
  val wiremock: ModuleID = "com.github.tomakehurst" % "wiremock"   % "2.26.0"

  val silencer = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % Version.silencer cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % Version.silencer % Provided cross CrossVersion.full
  )

  // Dependency Conflict Resolution
  val exclusions = Seq()
  val mccLibDependencyOverrides = Set()
}
