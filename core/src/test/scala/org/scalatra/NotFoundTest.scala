package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

class DefaultNotFoundTestServlet extends ScalatraServlet
class CustomNotFoundTestServlet extends ScalatraServlet {
  notFound {
    response.setStatus(404)
    "notFound block executed"
  }
}

class NotFoundTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(classOf[DefaultNotFoundTestServlet], "/default/*")
  addServlet(classOf[CustomNotFoundTestServlet], "/custom/*")
  
  test("executes notFound block") {
    get("/custom/intentionally-not-mapped") {
      status should equal (404)
      body should equal ("notFound block executed")
    }
  }

  test("default notFound block returns status 404") {
    get("/default/intentionally-not-mapped") {
      status should equal (404)
    }
  }
}

