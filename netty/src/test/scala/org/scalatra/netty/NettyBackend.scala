package org.scalatra
package netty

import org.scalatra.test.{FreePort, ScalatraTests}
import scalax.file.ImplicitConversions._
import store.session.InMemorySessionStore

trait NettyBackend { self: ScalatraTests =>

  val backend: WebServer = NettyServer(FreePort(), PublicDirectory("src/main/webapp"), SessionProvider(new InMemorySessionStore()))
}
