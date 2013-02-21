import sbt._
import Keys._
import Project._
import com.github.siasia.PluginKeys._
import com.github.siasia.WebPlugin._

object WarOverlayPlugin extends Plugin {

  object Keys {
    val overlayWars = TaskKey[Seq[File]]("overlay-wars", "Import the files from referenced wars")
  }

  import Keys._

  def overlayWarsTask: Initialize[Task[Seq[File]]] = {
    (sbt.Keys.update, target in (Compile, overlayWars), streams) map { (fcp, tgt, s) =>
      s.log.info("overlaying wars in classpath to " + tgt)
      if (!tgt.exists()) tgt.mkdirs()
      val mods = fcp.configuration(Compile.name).map(_.modules).getOrElse(Seq.empty)
      val wars = (mods map { r =>
        r.artifacts collect {
          case (Artifact(_, "war", "war", _, _, _, _), f) => f
        }
      }).flatten.distinct

      s.log.info("wars: " + wars)
      val allFiles = wars map (IO.unzip(_, tgt, "*" - "META-INF/*" - "WEB-INF/*", preserveLastModified = false))
      s.log.info("Unzipped: " + allFiles)
      if (allFiles.nonEmpty) allFiles.reduce(_ ++ _).toSeq
      else Seq.empty
    }
  }

  val warOverlaySettings: Seq[sbt.Setting[_]] = Seq(
    classpathTypes += "war",
    target in (Compile, overlayWars) <<= (target in Compile)(_ / "overlays"),
    overlayWars in Compile <<= overlayWarsTask,
    overlayWars <<= (overlayWars in Compile),
    webappResources in Compile <<= (target in (Compile, overlayWars), webappResources in Compile)(_ +: _),
    start in container.Configuration <<= (start in container.Configuration).dependsOn(overlayWars in Compile),
    packageWar in Compile <<= (packageWar in Compile).dependsOn(overlayWars in Compile)
  )
}