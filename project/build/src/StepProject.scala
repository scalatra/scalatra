import sbt._

class StepProject(info: ProjectInfo) extends ParentProject(info)
{
  override def shouldCheckOutputDirectories = false

  lazy val core = project("core", "step", new CoreProject(_)) 
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) {
    val jettytester = "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22" % "provided->default"
    val scalatest = "org.scalatest" % "scalatest" % "1.0" % "provided->default"
    val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"
  } 

  lazy val example = project("example", "step-example", new DefaultWebProject(_), core)
}
