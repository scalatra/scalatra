package org.scalatra.test

import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.http.UriCompliance
import org.eclipse.jetty.server.{ HttpConnectionFactory, Server, ServerConnector }
import org.eclipse.jetty.util.resource.ResourceFactory

import java.nio.file.{ Files, Paths }

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
  def localPort: Option[Int] = server.getConnectors collectFirst {
    case x: ServerConnector => x.getLocalPort
  }

  def contextPath = "/"

  lazy val server = new Server(port)

  lazy val servletContextHandler = {
    val handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.setContextPath(contextPath)
    handler.setBaseResource(
      ResourceFactory.of(handler).newResource(Paths.get(resourceBasePath)))
    handler.getServletHandler.setDecodeAmbiguousURIs(true)
    handler.setTempDirectory(Files.createTempDirectory("jetty").toFile)
    handler
  }

  def start(): Unit = {
    server.setHandler(servletContextHandler)
    server.getConnectors
      .head
      .getConnectionFactory(classOf[HttpConnectionFactory])
      .getHttpConfiguration
      .setUriCompliance(UriCompliance.LEGACY)
    server.start()
  }

  def stop(): Unit = server.stop()

  def baseUrl: String =
    server.getConnectors collectFirst {
      case conn: ServerConnector =>
        val host = Option(conn.getHost) getOrElse "localhost"
        val port = conn.getLocalPort
        require(port > 0, "The detected local port is < 1, that's not allowed")
        "http://%s:%d".format(host, port)
    } getOrElse sys.error("can't calculate base URL: no connector")
}
