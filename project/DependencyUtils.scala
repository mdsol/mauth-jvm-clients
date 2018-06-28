import sbt.ModuleID

trait DependencyUtils {
  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")

  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")

  def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")

  def feature(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "ft")

  def example(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "example")

  def integration(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "it")

  def cluster(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "multi-jvm")

  def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")

  def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")
}
