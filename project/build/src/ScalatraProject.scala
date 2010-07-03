import sbt._

class ScalatraProject(info: ProjectInfo) extends ParentProject(info)
{
  override def shouldCheckOutputDirectories = false

  val jettyGroupId = "org.mortbay.jetty"
  val jettyVersion = "6.1.22"

  lazy val core = project("core", "scalatra", new CoreProject(_)) 
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) {
    val jettytester = jettyGroupId % "jetty-servlet-tester" % jettyVersion % "provided"
    val scalatest = "org.scalatest" % "scalatest" % "1.0" % "provided->default"
    val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"
  } 

  lazy val example = project("example", "scalatra-example", new ExampleProject(_), core)
  class ExampleProject(info: ProjectInfo) extends DefaultWebProject(info) {
    val jetty6 = jettyGroupId % "jetty" % jettyVersion % "test"
  }
}
