import sbt._

class StepProject(info: ProjectInfo) extends DefaultWebProject(info)
{
  import BasicScalaProject._

  override def useMavenConfigurations = true
  override def packageAction = packageTask(mainClasses +++ mainResources, outputPath, defaultJarName, packageOptions).dependsOn(compile) describedAs PackageDescription

  val jettytester = "org.mortbay.jetty" % "jetty-servlet-tester" % "6.1.22" % "provided->default"
  val scalatest = "org.scala-tools.testing" % "scalatest" % "0.9.5" % "provided->default"
  val mockito = "org.mockito" % "mockito-core" % "1.8.2" % "test"
}
