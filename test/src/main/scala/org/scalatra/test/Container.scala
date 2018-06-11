package org.scalatra.test

trait Container {
  protected def ensureSessionIsSerializable(): Unit
  protected def start(): Unit
  protected def stop(): Unit
  var resourceBasePath: String = {
    val projectPath = getClass.getClassLoader.getResource("").toURI.resolve("../../..")
    s"${projectPath}/src/main/webapp"
  }
}
