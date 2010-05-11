import sbt._

class StepProject(info: ProjectInfo) extends ParentProject(info)
{
  override def shouldCheckOutputDirectories = false

  lazy val core = project("core", "step", new CoreProject(_)) 
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) {
    val jettytester = "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22" % "provided->default"
    val scalatest = "org.scalatest" % "scalatest" % scalatestVersion(crossScalaVersionString) % "provided->default"
    val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"
  } 

  lazy val example = project("example", "step-example", new DefaultWebProject(_), core)

  def scalatestVersion(scalaVersion: String) = {
    scalaVersion match {
      case "2.8.0.Beta1" => 
        "1.0.1-for-scala-2.8.0.Beta1-with-test-interfaces-0.3-SNAPSHOT"
      case "2.8.0.RC1" =>
        "1.0.1-for-scala-2.8.0.RC1-SNAPSHOT"
      case "2.8.0.RC2" =>
        "1.2-for-scala-2.8.0.RC2-SNAPSHOT"
    } 
  }

  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}
