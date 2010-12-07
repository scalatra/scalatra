package org.scalatra

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraSuite

class RemoveRouteServlet extends ScalatraServlet {
  val foo = get("/foo") { "foo" }

  post("/remove") {
    removeRoute("GET", foo)
  }

  notFound {
    "not found"
  }
}

class RemoveRouteSpec extends WordSpec with ScalatraSuite with ShouldMatchers {
  addServlet(classOf[RemoveRouteServlet], "/*")

  "a route" should {
    "not run" when {
      "it has been removed" in {
        get("/foo") {
          body should equal ("foo")
        }
        post("/remove") {}
        get("/foo") {
          body should equal ("not found")
        }
      }
    }
  }
}
