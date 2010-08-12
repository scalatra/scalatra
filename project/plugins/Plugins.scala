import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info)
{
  val gpgPlugin = "com.rossabaker" % "sbt-gpg-plugin" % "0.1.1"
}
