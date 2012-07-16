package org.scalatra
package netty

import org.scalatra.test.{FreePort, ScalatraTests}
import scalax.file.ImplicitConversions._
import store.session.InMemorySessionStore

trait NettyBackend { self: ScalatraTests =>

  lazy val backend: WebServer =
    new NettyServer(
          ServerInfo(
            "NettyTests",
            port = FreePort(),
            capabilities = Seq(PublicDirectory("src/main/webapp"), SessionProvider(new InMemorySessionStore())))) {
    }
}
