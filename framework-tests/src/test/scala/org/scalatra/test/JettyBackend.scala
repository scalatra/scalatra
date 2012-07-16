package org.scalatra
package test

import org.scalatra.jetty._
import org.scalatra.store.session.InMemorySessionStore

import scalax.file.ImplicitConversions._

trait JettyBackend { self: ScalatraTests =>

  lazy val backend: WebServer = JettyServer(FreePort(), PublicDirectory("src/main/webapp"), SessionProvider(new InMemorySessionStore()))
}
