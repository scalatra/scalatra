import sbt._

class StepProject(info: ProjectInfo) extends DefaultWebProject(info)
{
  import BasicScalaProject._

  override def useMavenConfigurations = true
  override def packageAction = packageTask(mainClasses +++ mainResources, outputPath, defaultJarName, packageOptions).dependsOn(compile) describedAs PackageDescription

  val jettytester = "org.mortbay.jetty" % "jetty-servlet-tester" % "7.0.0pre3" % "provided->default"
  val scalatest = "org.scalatest" % "scalatest" % "1.0.1-for-scala-2.8.0.Beta1-RC7-with-test-interfaces-0.3-SNAPSHOT" % "provided->default"
  val servlet = "org.mortbay.jetty" % "servlet-api" % "3.0.pre1" % "provided->default"

  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}
