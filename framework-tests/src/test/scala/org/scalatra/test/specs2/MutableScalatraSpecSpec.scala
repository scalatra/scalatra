package org.scalatra
package test
package specs2

abstract class MutableSalatraSpecSpec extends MutableScalatraSpec {

  sequential

  mount(new ScalatraApp {
    get("/") { "Hello, world." }
  })

  mount("/subcontext", new ScalatraApp {
    get("/") { "root" }
  })

  "get" should {
    "be able to verify the response body" in {
      get("/") {
        body must_== "Hello, world."
      }
    }
  }

  "get" should {
    "work with subcontexts" in {
      get("/subcontext") {
        body must_== "root"
      }

      get("/subcontext/") {
        body must_== "root"
      }
    }
  }
}

class NettyMutableSalatraSpecSpec extends MutableSalatraSpecSpec with NettyBackend
class JettyMutableSalatraSpecSpec extends MutableSalatraSpecSpec with JettyBackend
