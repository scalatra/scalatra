package org.scalatra.test

trait Container {
  protected def ensureSessionIsSerializable(): Unit
  protected def start(): Unit
  protected def stop(): Unit
  var resourceBasePath: String = "src/main/webapp"
}
