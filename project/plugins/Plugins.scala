import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info)
{
  val gpgPlugin = "com.rossabaker" % "sbt-gpg-plugin" % "0.1.1"

  val ideaPlugin = {
    val local = System.getenv("SOCIALINSIGHT_HOME")
    if(local == null || local.trim.length == 0 || local == ".") {
     "sbt-idea-repo" at "http://mpeltonen.github.com/maven/" 
    } else {
     "sbt-idea-repo" at "http://maven/content/repositories/thirdparty-snapshots"
    }
  }

  val idea = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.1-SNAPSHOT"
}
