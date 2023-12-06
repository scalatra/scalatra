package org.scalatra
package jetty

import java.net.InetSocketAddress
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.util.resource.ResourceFactory
import org.scalatra.servlet.ScalatraListener

import java.nio.file.Paths

/**
 * Runs a Servlet or a Filter on an embedded Jetty server.
 */
class JettyServer(
  socketAddress: InetSocketAddress = new InetSocketAddress(8080),
  resourceBase: String = "src/main/webapp") {
  val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
  context.setContextPath("/")
  context.addEventListener(new ScalatraListener)
  context.setBaseResource(
    ResourceFactory.of(context).newResource(Paths.get(resourceBase)))

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
