package org.scalatra
package netty

import org.scalatra.test.{FreePort, ScalatraTests}
import scalax.file.ImplicitConversions._

trait NettyBackend { self: ScalatraTests =>

  val backend: WebServer = NettyServer(FreePort(), PublicDirectory("src/main/webapp"))
}
