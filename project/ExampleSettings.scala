import sbt._

object ExampleSettings {
  lazy val ExampleResource = config("example") extend Compile
  lazy val exampleSettings = Seq(inConfig(ExampleResource)(Defaults.compileSettings) : _*)
}