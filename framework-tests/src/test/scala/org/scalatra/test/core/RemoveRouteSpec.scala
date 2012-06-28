package org.scalatra

import test.NettyBackend
import test.scalatest.ScalatraWordSpec

class RemoveRouteApp extends ScalatraApp {
  val foo = get("/foo") { "foo" }

  post("/remove") {
    removeRoute("GET", foo)
  }

}

abstract class RemoveRouteSpec extends ScalatraWordSpec {
  mount(new RemoveRouteApp)

  "a route" should {
    "not run" when {
      "it has been removed" in {
        get("/foo") {
          body should equal ("foo")
        }
        post("/remove") {}
        get("/foo") {
          body should equal ("Not Found")
        }
      }
    }
  }
}

class NettyRemoveRouteSpec extends RemoveRouteSpec with NettyBackend
