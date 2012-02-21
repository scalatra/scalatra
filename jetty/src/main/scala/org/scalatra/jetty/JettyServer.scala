package org.scalatra
package jetty

import java.net.InetSocketAddress
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler

import org.scalatra.servlet.{DefaultDispatchers, ScalatraListener}

/**
 * Runs a Servlet or a Filter on an embedded Jetty server.
 */
class JettyServer(
  socketAddress: InetSocketAddress = new InetSocketAddress(8080),
  resourceBase: String = "src/main/webapp")
{
  private val server = new Server(socketAddress)
  server.setHandler(context)

  val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
  context.setContextPath("/")
  context.addEventListener(new ScalatraListener)
  context.setResourceBase(resourceBase)
  server.setHandler(context)

  /**
   * Mounts a filter or a servlet to the given context.
   */
  def mount(mountable: Mountable[_], pathSpec: String): this.type = {
    mountable.mount(context, pathSpec)
    this
  }

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
