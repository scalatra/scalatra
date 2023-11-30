package org.scalatra

private[scalatra] object JettyCompat {
  type DefaultServlet = org.eclipse.jetty.servlet.DefaultServlet
  type FilterHolder = org.eclipse.jetty.servlet.FilterHolder
  type ServletContextHandler = org.eclipse.jetty.servlet.ServletContextHandler
  type ServletHolder = org.eclipse.jetty.servlet.ServletHolder

  def createServletContextHandler(resourceBase: String): ServletContextHandler = {
    val servletContextHandler = new org.eclipse.jetty.servlet.ServletContextHandler(org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS)
    servletContextHandler.setBaseResource(servletContextHandler.newResource(resourceBase))
    servletContextHandler
  }
}
