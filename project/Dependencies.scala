import sbt._

object Dependencies extends DependencyUtils {

  private object Version {
    // Careful with upgrading akka due to license change https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka
    val akka: String = "2.6.20" // Do not update beyond 2.6.* due to license changes
    val akkaHttp: String = "10.2.10" // Do not update beyond 10.2.* due to license changes
    val logback = "1.5.6"
    val silencer: String = "1.6.0"
  }

  val akkaHttp: ModuleID = "com.typesafe.akka"                 %% "akka-http"        % Version.akkaHttp
  val akkaStream: ModuleID = "com.typesafe.akka"               %% "akka-stream"      % Version.akka
  val apacheHttpClient: ModuleID = "org.apache.httpcomponents" % "httpclient"        % "4.5.14"
  val bouncyCastlePkix: ModuleID = "org.bouncycastle"          % "bcpkix-jdk15on"    % "1.70"
  val commonsCodec: ModuleID = "commons-codec"                 % "commons-codec"     % "1.17.0"
  val commonsLang3: ModuleID = "org.apache.commons"            % "commons-lang3"     % "3.9"
  val guava: ModuleID = "com.google.guava"                     % "guava"             % "23.0"
  val jacksonDataBind: ModuleID = "com.fasterxml.jackson.core" % "jackson-databind"  % "2.17.0"
  val littleProxy: ModuleID = "org.littleshoot"                % "littleproxy"       % "1.1.2"
  val logbackClassic: ModuleID = "ch.qos.logback"              % "logback-classic"   % Version.logback
  val logbackCore: ModuleID = "ch.qos.logback"                 % "logback-core"      % Version.logback
  val slf4jApi: ModuleID = "org.slf4j"                         % "slf4j-api"         % "2.0.12"
  val typeSafeConfig: ModuleID = "com.typesafe"                % "config"            % "1.4.3"
  val scalaCache: ModuleID = "com.github.cb372"                %% "scalacache-guava" % "0.28.0"
  val scalaLogging: ModuleID = "com.typesafe.scala-logging"    %% "scala-logging"    % "3.9.5"

  // TEST DEPENDENCIES
  // Not sure why they don't make the akka-http test kit depends on other test kits...
  val akkaHttpTestKit: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http-testkit"   % Version.akkaHttp,
    "com.typesafe.akka" %% "akka-testkit"        % Version.akka,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
  )
  val commonsIO: ModuleID = "commons-io"            % "commons-io" % "2.16.1"
  val scalaMock: ModuleID = "org.scalamock"         %% "scalamock" % "4.4.0"
  val scalaTest: ModuleID = "org.scalatest"         %% "scalatest" % "3.2.19"
  val wiremock: ModuleID = "com.github.tomakehurst" % "wiremock"   % "2.26.0"

  val silencer: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % Version.silencer cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % Version.silencer % Provided cross CrossVersion.full
  )

  // Dependency Conflict Resolution
  val exclusions: Seq[ExclusionRule] = Seq()
  val mccLibDependencyOverrides: Set[ModuleID] = Set()
}
