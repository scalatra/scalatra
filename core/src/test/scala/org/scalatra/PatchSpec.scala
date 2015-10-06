package org.scalatra

import org.scalatra.test.scalatest.ScalatraWordSpec

class PatchSpec extends ScalatraWordSpec {
  addServlet(new ScalatraServlet {
    patch("/") {
      params.get("name").getOrElse("params are absent")
    }
  }, "/*")

  "Patch requests " should {
    "have valid request body" in {
      patch("/", "name" -> "Scala") {
        body should equal("Scala")
        status should equal(200)
      }
    }
  }
}
