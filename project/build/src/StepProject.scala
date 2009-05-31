import sbt._

class StepProject(info: ProjectInfo) extends DefaultWebProject(info)
{
  import BasicScalaProject._

  override def useMavenConfigurations = true
  override def packageAction = packageTask(mainClasses +++ mainResources, outputPath, defaultJarName, packageOptions).dependsOn(compile) describedAs PackageDescription

  val jetty = "org.mortbay.jetty" % "jetty" % "6.1.14" % "test->default"
  val jettytester = "org.eclipse.jetty" % "jetty-servlet-tester" % "7.0.0.M1" % "test->default"
  val scalatest = "org.scala-tools.testing" % "scalatest" % "0.9.5" % "test->default"
  val servlet = "javax.servlet" % "servlet-api" % "2.5" % "provided->default"
}
