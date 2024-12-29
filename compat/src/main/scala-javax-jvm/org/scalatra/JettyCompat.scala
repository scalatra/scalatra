package org.scalatra

import org.eclipse.jetty.util.resource.PathResource

import java.nio.file.Path

private[scalatra] object JettyCompat {
  type DefaultServlet        = org.eclipse.jetty.servlet.DefaultServlet
  type FilterHolder          = org.eclipse.jetty.servlet.FilterHolder
  type ServletContextHandler = org.eclipse.jetty.servlet.ServletContextHandler
  type ServletHolder         = org.eclipse.jetty.servlet.ServletHolder

  def createServletContextHandler(resourceBasePath: Path): ServletContextHandler = {
    val servletContextHandler =
      new org.eclipse.jetty.servlet.ServletContextHandler(org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS)
    servletContextHandler.setBaseResource(new PathResource(resourceBasePath))
    servletContextHandler
  }
}
