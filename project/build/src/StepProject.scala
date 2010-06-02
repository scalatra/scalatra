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

  lazy val scalate = project("scalate", "step-scalate", new ScalateProject(_), core)
  class ScalateProject(info: ProjectInfo) extends DefaultProject(info) {
    val scalate = "org.fusesource.scalate" % "scalate-core" % "1.2-scala-next-SNAPSHOT"
  }

  lazy val example = project("example", "step-example", new ExampleProject(_), core, fileupload, scalate)
  class ExampleProject(info: ProjectInfo) extends DefaultWebProject(info) {
    val jetty6 = jettyGroupId % "jetty" % jettyVersion % "test"
    val logback = "ch.qos.logback" % "logback-classic" % "0.9.21" % "runtime" // Scalate needs a slf4j binding.
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

  val fuseSourceSnapshots = "FuseSource Snapshot Repository" at "http://repo.fusesource.com/nexus/content/repositories/snapshots"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}
