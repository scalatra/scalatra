package org.scalatra

import org.eclipse.jetty.util.resource.ResourceFactory

import java.nio.file.Path

private[scalatra] object JettyCompat {
  type DefaultServlet        = org.eclipse.jetty.ee11.servlet.DefaultServlet
  type FilterHolder          = org.eclipse.jetty.ee11.servlet.FilterHolder
  type ServletContextHandler = org.eclipse.jetty.ee11.servlet.ServletContextHandler
  type ServletHolder         = org.eclipse.jetty.ee11.servlet.ServletHolder

  def createServletContextHandler(resourceBasePath: Path): ServletContextHandler = {
    val servletContextHandler = new org.eclipse.jetty.ee11.servlet.ServletContextHandler(
      org.eclipse.jetty.ee11.servlet.ServletContextHandler.SESSIONS
    )
    servletContextHandler.setBaseResource(ResourceFactory.of(servletContextHandler).newResource(resourceBasePath))
    servletContextHandler
  }
}
