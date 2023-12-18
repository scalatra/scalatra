package org.scalatra.test

import org.eclipse.jetty.server.Server
import org.scalatra.JettyCompat.ServletContextHandler

private[scalatra] object EmbeddedJettyContainerCompat {
  def configureServletContextHandler(handler: ServletContextHandler) = {}

  def configureServer(server: Server) = {}
}
