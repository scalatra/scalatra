package org.scalatra.tomcat

import org.apache.catalina.Context
import org.apache.catalina.startup.Tomcat
import org.scalatra.servlet.ScalatraListener

/**
 * Runs a Servlet or a Filter on an embedded Tomcat server.
 */
class TomcatServer(
  hostname: String = "localhost",
  port: Int = 8080,
  docBase: String = "src/main/webapp") {
  val server = new Tomcat()
  server.setHostname(hostname)
  server.setPort(port)

  val context: Context = server.addContext("", docBase)
  context.addApplicationListener(classOf[ScalatraListener].getName)

  def start(): this.type = {
    server.start()
    this
  }

  def stop(): this.type = {
    server.stop()
    this
  }

  def join(): this.type = {
    server.getServer.await()
    this
  }

}
