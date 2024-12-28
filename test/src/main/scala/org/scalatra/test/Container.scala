package org.scalatra.test

import java.nio.file.{Path, Paths}

trait Container {
  protected def ensureSessionIsSerializable(): Unit
  protected def start(): Unit
  protected def stop(): Unit
  var resourceBasePath: Path = {
    val projectPath = Paths.get(
      getClass.getClassLoader.getResource("").toURI.resolve("../../..")
    )
    val webAppPath = projectPath.resolve("src/main/webapp")
    if (webAppPath.toFile.isDirectory) {
      webAppPath
    } else {
      projectPath
    }
  }
}
