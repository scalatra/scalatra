import sbt._

class StepProject(info: ProjectInfo) extends ParentProject(info)
{

  val jettytester = "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22" % "provided->default"
  val scalatest = "org.scalatest" % "scalatest" % "1.0" % "provided->default"
  val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"

  override def shouldCheckOutputDirectories = false
  lazy val stepProject = project(".", "step")
  lazy val stepExampleProject = project("example-step-project", "example-step-project", new DefaultWebProject(_), stepProject)
}
