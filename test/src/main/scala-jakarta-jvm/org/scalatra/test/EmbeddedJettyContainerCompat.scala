package org.scalatra.test

import org.eclipse.jetty.http.UriCompliance
import org.eclipse.jetty.server.{ HttpConnectionFactory, Server }
import org.scalatra.JettyCompat.ServletContextHandler

import java.nio.file.Files

private[scalatra] object EmbeddedJettyContainerCompat {
  def configureServletContextHandler(handler: ServletContextHandler) = {
    handler.getServletHandler.setDecodeAmbiguousURIs(true)
    handler.setTempDirectory(Files.createTempDirectory("jetty").toFile)
  }

  def configureServer(server: Server) = {
    server.getConnectors
      .head
      .getConnectionFactory(classOf[HttpConnectionFactory])
      .getHttpConfiguration
      .setUriCompliance(UriCompliance.LEGACY)
  }
}
