package org.scalatra

import test.specs.ScalatraSpecification
import org.specs._

class ScalatraSpecificationTestServlet extends ScalatraServlet {
  get("/") { "ScalatraSpecification works" }
}

object ScalatraSpecificationTest extends ScalatraSpecification {
  addServlet(classOf[ScalatraSpecificationTestServlet], "/")

  "GET /" should {
    "say 'ScalatraSpecification works'" in {
      get("/") {
        body must_== "ScalatraSpecification works"
      }
    }
  }
}