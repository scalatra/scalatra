package org.scalatra.test

import javax.servlet.Filter
import javax.servlet.http.HttpServlet
import java.util.EnumSet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import com.weiglewilczek.slf4s.Logger

trait EmbeddedJettyContainer extends JettyContainer {
  private val logger = Logger(getClass)

  /**
   * Sets the port to listen on.  0 means listen on any available port.
   */
  def port: Int = 0

  /**
   * The port of the currently running Jetty.  May differ from port if port is 0.
   * 
   * @return Some port if Jetty is currently listening, or None if it is not.
   */
  def currentPort: Option[Int] = _currentPort
  private var _currentPort: Option[Int] = None

  lazy val server = new Server(port)

  lazy val servletContextHandler = {
    val handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.setContextPath("/")
    handler
  }

  def start(): Unit = {
    server.setHandler(servletContextHandler)
    server.start()
    _currentPort = server.getConnectors.headOption map { _.getLocalPort }
  }

  def stop(): Unit = {
    _currentPort = None
    server.stop()
  }
}
