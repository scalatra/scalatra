import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info)
{
  val scalatePlugin = "org.fusesource.scalate" % "sbt-scalate-plugin" % "1.4.1"

  val snuggletex_repo = "snuggletex_repo" at "http://www2.ph.ed.ac.uk/maven2"
  val posterous = "net.databinder" % "posterous-sbt" % "0.1.7"
}
