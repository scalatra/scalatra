package org.scalatra
package test

import org.scalatra.jetty._
import scalax.file.ImplicitConversions._

trait JettyBackend { self: ScalatraTests =>

  lazy val backend: WebServer = JettyServer(FreePort(), PublicDirectory("src/main/webapp"))
}
