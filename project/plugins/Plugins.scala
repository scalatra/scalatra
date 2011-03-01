import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info)
{
  val scalatePlugin = "org.fusesource.scalate" % "sbt-scalate-plugin" % "1.4.1"
}
