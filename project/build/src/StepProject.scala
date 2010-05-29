import sbt._

class StepProject(info: ProjectInfo) extends ParentProject(info)
{
  override def shouldCheckOutputDirectories = false

  val jettyGroupId = "org.mortbay.jetty"
  val jettyVersion = "6.1.22"

  lazy val core = project("core", "step", new CoreProject(_)) 
  class CoreProject(info: ProjectInfo) extends DefaultProject(info) {
    val jettytester = jettyGroupId % "jetty-servlet-tester" % jettyVersion % "provided"
    val scalatest = "org.scalatest" % "scalatest" % scalatestVersion(crossScalaVersionString) % "provided->default"
    val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"
  } 

  lazy val fileupload = project("fileupload", "step-fileupload", new FileuploadProject(_), core) 
  class FileuploadProject(info: ProjectInfo) extends DefaultProject(info) {
    val commonsFileupload = "commons-fileupload" % "commons-fileupload" % "1.2.1" % "compile"
    val commonsIo = "commons-io" % "commons-io" % "1.4" % "compile"
  } 

  lazy val example = project("example", "step-example", new ExampleProject(_), core, fileupload)
  class ExampleProject(info: ProjectInfo) extends DefaultWebProject(info) {
    val jetty6 = jettyGroupId % "jetty" % jettyVersion % "test"
  }

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
