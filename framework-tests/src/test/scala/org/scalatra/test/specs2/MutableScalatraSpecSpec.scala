package org.scalatra
package test
package specs2

abstract class MutableSalatraSpecSpec extends MutableScalatraSpec {

  mount(new ScalatraApp {
    get("/") { "Hello, world." }
  })


  "get" should {
    "be able to verify the response body" in {
      get("/") {
        body must_== "Hello, world."
      }
    }
  }
}

class NettyMutableSalatraSpecSpec extends MutableSalatraSpecSpec with NettyBackend