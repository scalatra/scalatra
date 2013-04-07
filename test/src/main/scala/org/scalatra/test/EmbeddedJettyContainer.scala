package org.scalatra.test

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ServletContextHandler

trait EmbeddedJettyContainer extends JettyContainer {
  /**
   * Sets the port to listen on.  0 means listen on any available port.
   */
  def port: Int = 0

  /**
   * The port of the currently running Jetty.  May differ from port if port is 0.
   *
   * @return Some port if Jetty is currently listening, or None if it is not.
   */
  def localPort: Option[Int] = server.getConnectors.headOption map {
    case conn: ServerConnector => conn.getLocalPort
    case _ => sys.error("can't get local port")
  }

  def contextPath = "/"

  lazy val server = new Server(port)

  lazy val servletContextHandler = {
    val handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.setContextPath(contextPath)
    handler.setResourceBase(resourceBasePath)
    handler
  }

  def start(): Unit = {
    server.setHandler(servletContextHandler)
    server.start()
  }

  def stop(): Unit = server.stop()

  def baseUrl: String =
    server.getConnectors.headOption match {
      case Some(conn: ServerConnector) =>
        val host = Option(conn.getHost) getOrElse "localhost"
        val port = conn.getLocalPort
        require(port > 0, "The detected local port is < 1, that's not allowed")
        "http://%s:%d".format(host, port)
      case _ =>
        sys.error("can't calculate base URL: no connector")
    }
}

