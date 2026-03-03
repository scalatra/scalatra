package org.scalatra.test

import java.nio.file.{Path, Paths}
import scala.jdk.CollectionConverters.*

trait Container {
  protected def ensureSessionIsSerializable(): Unit
  protected def start(): Unit
  protected def stop(): Unit
  var resourceBasePath: Path = {
    val urls    = getClass.getClassLoader.getResources("").asScala.toSeq
    val fileUrl = urls.find(_.getProtocol == "file")

    fileUrl match {
      case Some(url) =>
        val projectPath = Paths.get(url.toURI).resolve("../../..")
        val webAppPath  = projectPath.resolve("src/main/webapp")
        if (webAppPath.toFile.isDirectory) webAppPath else projectPath
      case None =>
        // Fallback for JMH/JAR contexts where file URLs aren't available
        Paths.get(System.getProperty("user.dir"))
    }
  }
}
