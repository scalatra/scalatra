package org.scalatra

import java.nio.file.Paths
import org.eclipse.jetty.util.resource.ResourceFactory

private[scalatra] object JettyCompat {
  type DefaultServlet = org.eclipse.jetty.ee10.servlet.DefaultServlet
  type FilterHolder = org.eclipse.jetty.ee10.servlet.FilterHolder
  type ServletContextHandler = org.eclipse.jetty.ee10.servlet.ServletContextHandler
  type ServletHolder = org.eclipse.jetty.ee10.servlet.ServletHolder

  def createServletContextHandler(resourceBase: String): ServletContextHandler = {
    val servletContextHandler = new org.eclipse.jetty.ee10.servlet.ServletContextHandler(org.eclipse.jetty.ee10.servlet.ServletContextHandler.SESSIONS)
    servletContextHandler.setBaseResource(ResourceFactory.of(servletContextHandler).newResource(Paths.get(resourceBase)))
    servletContextHandler
  }
}
