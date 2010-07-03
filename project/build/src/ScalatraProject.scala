import sbt._

class ScalatraProject(info: ProjectInfo) extends ParentProject(info)
{
  override def shouldCheckOutputDirectories = false

  val jettyGroupId = "org.mortbay.jetty"
  val jettyVersion = "6.1.22"

  lazy val core = project("core", "scalatra", new CoreProject(_)) 
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) {
    val jettytester = jettyGroupId % "jetty-servlet-tester" % jettyVersion % "provided"
    val scalatest = "org.scalatest" % "scalatest" % scalatestVersion(crossScalaVersionString) % "provided->default"
    val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"
  } 

  lazy val example = project("example", "scalatra-example", new ExampleProject(_), core)
  class ExampleProject(info: ProjectInfo) extends DefaultWebProject(info) {
    val jetty6 = jettyGroupId % "jetty" % jettyVersion % "test"
  }

  def scalatestVersion(scalaVersion: String) = {
    scalaVersion match {
      case "2.8.0.Beta1" => 
        "1.0.1-for-scala-2.8.0.Beta1-with-test-interfaces-0.3-SNAPSHOT"
      case "2.8.0.RC1" =>
        "1.0.1-for-scala-2.8.0.RC1-SNAPSHOT"
      case x =>
        "1.2-for-scala-"+x+"-SNAPSHOT"
    } 
  }

  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}
