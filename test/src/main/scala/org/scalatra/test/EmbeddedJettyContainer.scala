package org.scalatra.test

import javax.servlet.Filter
import javax.servlet.http.HttpServlet
import java.util.EnumSet
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler

trait EmbeddedJettyContainer extends JettyContainer {
  lazy val port: Int = 8080
  lazy val server = new Server(port)

  lazy val servletContextHandler = {
    val handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.setContextPath("/")
    handler
  }

  def start() = {
    server.setHandler(servletContextHandler)
    server.start()
  }

  def stop() = server.stop()
}
