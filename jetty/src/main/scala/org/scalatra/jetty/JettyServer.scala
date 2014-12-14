package org.scalatra
package jetty

import java.net.InetSocketAddress
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler

import org.scalatra.servlet.ScalatraListener

/**
 * Runs a Servlet or a Filter on an embedded Jetty server.
 */
class JettyServer(
    socketAddress: InetSocketAddress = new InetSocketAddress(8080),
    resourceBase: String = "src/main/webapp") {
  val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
  context.setContextPath("/")
  context.addEventListener(new ScalatraListener)
  context.setResourceBase(resourceBase)

  val server = new Server(socketAddress)
  server.setHandler(context)

  def start(): this.type = {
    server.start()
    this
  }

  def stop(): this.type = {
    server.stop()
    this
  }

  def join(): this.type = {
    server.join
    this
  }
}
