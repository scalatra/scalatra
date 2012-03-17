package org.scalatra.test

import org.eclipse.jetty.server.Server
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
  def localPort: Option[Int] = server.getConnectors.headOption map { _.getLocalPort }

  lazy val server = new Server(port)

  lazy val servletContextHandler = {
    val handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.setContextPath("/")
    handler
  }

  def start(): Unit = {
    server.setHandler(servletContextHandler)
    server.start()
  }

  def stop(): Unit = server.stop()

  def baseUrl: String =
    server.getConnectors.headOption match {
      case Some(conn) => 
        val host = Option(conn.getHost) getOrElse "localhost"
        val port = conn.getLocalPort
        "http://%s:%d".format(host, port)
      case None =>
        sys.error("can't calculate base URL: no connector")
    }
}

