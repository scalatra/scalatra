package org.scalatra
package netty

import java.net.{URI, URL}

object NettyAppContext {
  def apply(server: ServerInfo, applications: AppMounter.ApplicationRegistry) = new NettyAppContext(server, applications)
}
class NettyAppContext(server: ServerInfo, applications: AppMounter.ApplicationRegistry) extends AppContextBase(server, applications) {
  def resourceFor(path: String): URL = URI.create("file://" + absolutizePath(path)).toURL

  def physicalPath(uri: String): String = resourceFor(uri).toURI.getSchemeSpecificPart
}
