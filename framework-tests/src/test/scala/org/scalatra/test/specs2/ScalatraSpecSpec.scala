package org.scalatra
package test
package specs2

import netty.NettyServer

abstract class ScalatraSpecSpec extends ScalatraSpec { def is =
  "get / should"                               ^
    "return 'Hello, world.'"                   ! e1


  mount(new ScalatraApp {
    protected var doNotFound: _root_.org.scalatra.Action = () => NotFound()
    get("/") { "Hello, world." }
  })



  def e1 = get("/") {
    body must_== "Hello, world."
  }
}

class NettyScalatraSpecSpec extends ScalatraSpecSpec with NettyBackend