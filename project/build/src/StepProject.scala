import sbt._

class StepProject(info: ProjectInfo) extends DefaultWebProject(info)
{
  import BasicScalaProject._

  override def useMavenConfigurations = true
  override def packageAction = packageTask(mainClasses +++ mainResources, outputPath, defaultJarName, packageOptions).dependsOn(compile) describedAs PackageDescription

  val jettytester = "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22" % "provided->default"
  val scalatest = "org.scalatest" % "scalatest" % "1.0.1-for-scala-2.8.0.Beta1-with-test-interfaces-0.3-SNAPSHOT" % "provided->default"
  val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"
  val scalate = "org.fusesource.scalate" % "scalate-core"  % "1.0-SNAPSHOT"
  val scalateReop = "scalate repo" at "http://scalate.fusesource.org/repo/snapshot/"
  val scalaToolsSnapshots = "Scala-Tools Maven2 Snapshots Repository" at "http://scala-tools.org/repo-snapshots"
}
