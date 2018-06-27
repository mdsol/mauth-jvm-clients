import sbt._

object ExampleTesting {
  lazy val ExampleTests = config("example") extend (Test)
  lazy val exampleSettings = Seq(inConfig(ExampleTests)(Defaults.testSettings) : _*)
}