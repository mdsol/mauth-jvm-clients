import sbt._

object Dependencies extends DependencyUtils {

  private object Version {
    // Careful with upgrading akka due to license change https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka
    val akka: String = "2.6.20" // Do not update beyond 2.6.* due to license changes
    val akkaHttp: String = "10.2.10" // Do not update beyond 10.2.* due to license changes
    val logback = "1.5.6"
    val sttp = "3.10.2"
    val http4s = "0.23.29"
    val enumeratum = "1.7.4"
    val log4cats = "2.7.0"
    val circe = "0.14.9"
    val circeGenericExtras = "0.14.3"
    val http4s022 = "0.22.15"
  }

  val akkaHttp: ModuleID = "com.typesafe.akka"                          %% "akka-http"               % Version.akkaHttp
  val akkaHttpCache: ModuleID = "com.typesafe.akka"                     %% "akka-http-caching"       % Version.akkaHttp
  val akkaStream: ModuleID = "com.typesafe.akka"                        %% "akka-stream"             % Version.akka
  val apacheHttpClient: ModuleID = "org.apache.httpcomponents"           % "httpclient"              % "4.5.14"
  val bouncyCastlePkix: ModuleID = "org.bouncycastle"                    % "bcpkix-jdk18on"          % "1.78.1"
  val commonsCodec: ModuleID = "commons-codec"                           % "commons-codec"           % "1.17.0"
  val commonsLang3: ModuleID = "org.apache.commons"                      % "commons-lang3"           % "3.14.0"
  val guava: ModuleID = "com.google.guava"                               % "guava"                   % "31.1-jre"
  val jacksonDataBind: ModuleID = "com.fasterxml.jackson.core"           % "jackson-databind"        % "2.17.0"
  val logbackClassic: ModuleID = "ch.qos.logback"                        % "logback-classic"         % Version.logback
  val logbackCore: ModuleID = "ch.qos.logback"                           % "logback-core"            % Version.logback
  val slf4jApi: ModuleID = "org.slf4j"                                   % "slf4j-api"               % "2.0.12"
  val typeSafeConfig: ModuleID = "com.typesafe"                          % "config"                  % "1.4.3"
  val scalaCacheCore: ModuleID = "com.github.cb372"                     %% "scalacache-core"         % "1.0.0-M6"
  val scalaLogging: ModuleID = "com.typesafe.scala-logging"             %% "scala-logging"           % "3.9.5"
  val catsEffect: ModuleID = "org.typelevel"                            %% "cats-effect"             % "3.4.8"
  val sttp: ModuleID = "com.softwaremill.sttp.client3"                  %% "core"                    % Version.sttp
  val sttpAkkaHttpBackend: ModuleID = "com.softwaremill.sttp.client3"   %% "akka-http-backend"       % Version.sttp
  val sttpHttp4sHttpBackend: ModuleID = "com.softwaremill.sttp.client3" %% "http4s-backend"          % Version.sttp
  val sttpFs2: ModuleID = "com.softwaremill.sttp.shared"                %% "fs2"                     % "1.4.2"
  val scalaLibCompat: ModuleID = "org.scala-lang.modules"               %% "scala-collection-compat" % "2.12.0"
  val caffeine: ModuleID = "com.github.ben-manes.caffeine"               % "caffeine"                % "3.1.8"
  val http4sDsl: ModuleID = "org.http4s"                                %% "http4s-dsl"              % Version.http4s
  val http4sDsl022: ModuleID = "org.http4s"                             %% "http4s-dsl"              % Version.http4s022
  val http4sClient: ModuleID = "org.http4s"                             %% "http4s-client"           % Version.http4s
  val http4sClient022: ModuleID = "org.http4s"                          %% "http4s-client"           % Version.http4s022
  val http4sEmberClient: ModuleID = "org.http4s"                        %% "http4s-ember-client"     % Version.http4s
  val enumeratum: ModuleID = "com.beachape"                             %% "enumeratum"              % Version.enumeratum
  val log4cats: ModuleID = "org.typelevel"                              %% "log4cats-slf4j"          % Version.log4cats

  lazy val circeBasic: Seq[ModuleID] = Seq(
    "io.circe"   %% "circe-core"           % Version.circe,
    "io.circe"   %% "circe-parser"         % Version.circe,
    "io.circe"   %% "circe-generic-extras" % Version.circeGenericExtras,
    "org.http4s" %% "http4s-circe"         % Version.http4s
  )

  // TEST DEPENDENCIES
  val akkaHttpTestKit: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-http-testkit"   % Version.akkaHttp,
    "com.typesafe.akka" %% "akka-testkit"        % Version.akka,
    "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
  )
  val commonsIO: ModuleID = "commons-io"                 % "commons-io"          % "2.16.1"
  val scalaMock: ModuleID = "org.scalamock"             %% "scalamock"           % "5.2.0"
  val scalaTest: ModuleID = "org.scalatest"             %% "scalatest"           % "3.2.19"
  val wiremock: ModuleID = "com.github.tomakehurst"      % "wiremock"            % "2.27.2"
  val munitCatsEffect: ModuleID = "org.typelevel"       %% "munit-cats-effect-3" % "1.0.7"
  val munitCatsEffect2: ModuleID = "org.typelevel"      %% "munit-cats-effect-2" % "1.0.7"
  val log4catsNoop: ModuleID = "org.typelevel"          %% "log4cats-noop"       % Version.log4cats
  val scalaCacheCaffeine: ModuleID = "com.github.cb372" %% "scalacache-caffeine" % "1.0.0-M6"

  // Dependency Conflict Resolution
  val exclusions = Seq()
  val mccLibDependencyOverrides = Set()
}
