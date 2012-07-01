package org.scalatra
package jetty

import java.net.InetSocketAddress
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet._

import org.scalatra.servlet._

object JettyServer extends WebServerFactory {
  val DefaultServerName = "ScalatraJettyHttpServer"
  
  def apply(capabilities: ServerCapability*): WebServer = {
    JettyServer(ServerInfo(DefaultServerName, capabilities = capabilities))  
  }
  
  def apply(port: Int,  capabilities: ServerCapability*): WebServer = {
    JettyServer(ServerInfo(DefaultServerName, port = port, capabilities = capabilities))
  }
  
  def apply(base: String, capabilities: ServerCapability*): WebServer = {
    JettyServer(ServerInfo(DefaultServerName, base = base, capabilities = capabilities))
  }
  
  def apply(port: Int, base: String, capabilities: ServerCapability*): WebServer = {
    JettyServer(ServerInfo(DefaultServerName, port = port, base = base, capabilities = capabilities))
  }
  
}

/**
 * Runs the ScalatraServlet on an embedded Jetty server.
 */
case class JettyServer(info: ServerInfo) extends WebServer {

  private val server = new Server(new InetSocketAddress(port))

  val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
  context.setContextPath("/")
  // context.addEventListener(new ScalatraListener)
  context.addServlet(new ServletHolder(new ScalatraServlet(appContext)), "/")
  context.setResourceBase(info.base)
  server.setHandler(context)

  // override def mount[TheApp <: Mountable](app: => TheApp): AppMounter = {
  //   val mounter = super.mount("/", app)
  //   mounter
  // }

  onStart {
    logger info ("Starting Jetty HTTP server on %d" format port)
    server.start
  }

  onStop {
    server.stop
    appContext.applications.valuesIterator foreach (_.mounted.destroy())
    logger info ("Jetty HTTP server on %d stopped." format port)
  }
}
