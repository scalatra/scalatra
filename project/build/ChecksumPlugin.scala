import sbt._
import Process._
import scala.xml._
import com.rossabaker.sbt.gpg._
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.DigestInputStream

// TODO make independent of GpgPlugin
trait ChecksumPlugin extends BasicManagedProject with GpgPlugin {
  lazy val skipChecksum = systemOptional[Boolean]("checksum.skip", false).value
  val checksumsConfig = config("checksums") 

  override def artifacts = 
    if (skipChecksum)
      super.artifacts
    else 
      super.artifacts flatMap { artifact =>
        artifact.`type` match {
          case "asc" => Seq(artifact)
          case _ => artifact :: (List("md5", "sha1").map { ext =>
            Artifact(artifact.name, ext, artifact.extension+"."+ext, 
              artifact.classifier, Seq(checksumsConfig), None)
          })
        }
      }

  lazy val checksum = checksumAction

  def checksumAction = checksumTask(artifacts) 
    .dependsOn(makePom)
    .describedAs("Calculates MD5 and SHA1 checksums")

  def checksumTask(artifacts: Iterable[Artifact]): Task = task {
    if (skipChecksum) {
      log.info("Skipping checksums")
      None
    }
    else {
      artifacts.toStream flatMap checksumArtifact firstOption
    }
  }

  def checksumArtifact(artifact: Artifact): Option[String] = {
    val path = artifact2Path(artifact)
    path.ext match {
      case "asc" => None
      case "md5" => None
      case "sha1" => None
      case _ =>
        Stream("md5", "sha1").flatMap { ext =>
          val md = MessageDigest.getInstance(ext);
          val is = new FileInputStream(path asFile);
          try {
            // inefficient and ugly 
            val dis = new DigestInputStream(is, md);
            while ( dis.read != -1) {
              // this updates the associated digest
            }
            val data = dis.getMessageDigest.digest
            val checksum = data map (x => "%02x".format(x)) mkString
            
            val outfile = path+"."+ext
            log.info("Writing checksum to "+outfile)
            FileUtilities.write(new File(outfile), checksum, log)
          }
          finally {
            is close;
          }
        }.firstOption
    }
  }

  override def deliverLocalAction = super.deliverLocalAction dependsOn(checksum)
  override def deliverAction = super.deliverAction dependsOn(checksum)
}
