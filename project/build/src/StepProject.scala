import sbt._

class StepProject(info: ProjectInfo) extends DefaultWebProject(info)
{
  import BasicScalaProject._

  override def useMavenConfigurations = true
  override def packageAction = packageTask(mainClasses +++ mainResources, outputPath, defaultJarName, packageOptions).dependsOn(compile) describedAs PackageDescription

  val jettytester = "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22" % "provided->default"
  val scalatest = "org.scalatest" % "scalatest" % scalatestVersion(crossScalaVersionString) % "provided->default"
  val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"

  def scalatestVersion(scalaVersion: String) = {
    val qualifier = scalaVersion match {
      case "2.8.0.Beta1" => "scala-2.8.0.Beta1-with-test-interfaces-0.3"
      case x => x
    } 
    "1.0.1-for-scala-"+qualifier+"-SNAPSHOT"
  }

  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}
