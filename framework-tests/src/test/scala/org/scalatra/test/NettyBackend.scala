package org.scalatra
package test

import netty.NettyServer
import scalax.file.ImplicitConversions._
import store.session.InMemorySessionStore

trait NettyBackend { self: ScalatraTests =>

  lazy val backend = new NettyServer(
        ServerInfo(
          "NettyTests",
          port = FreePort(),
          capabilities = Seq(PublicDirectory("src/main/webapp"), SessionProvider(new InMemorySessionStore())))) {
  }
}
