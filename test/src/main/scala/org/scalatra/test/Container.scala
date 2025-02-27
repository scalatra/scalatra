package org.scalatra.test

import java.nio.file.{Path, Paths}
import scala.jdk.CollectionConverters.*

trait Container {
  protected def ensureSessionIsSerializable(): Unit
  protected def start(): Unit
  protected def stop(): Unit
  var resourceBasePath: Path = {
    val urls = getClass.getClassLoader.getResources("").asScala.toSeq
    val fileUrl = urls.find(_.getProtocol == "file").getOrElse(
      throw new IllegalStateException("No file URL found in class loader resources")
    )
    val projectPath = Paths.get(fileUrl.toURI).resolve("../../..")
    val webAppPath  = projectPath.resolve("src/main/webapp")
    if (webAppPath.toFile.isDirectory) {
      webAppPath
    } else {
      projectPath
    }
  }
}
