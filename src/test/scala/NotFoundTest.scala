package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class CustomNotFoundTestServlet extends Step {
  notFound {
    response.setStatus(404)
    "notFound block executed"
  }
}

class NotFoundTest extends StepSuite with ShouldMatchers {
  route(classOf[CustomNotFoundTestServlet], "/*")
  
  test("executes notFound block") {
    get("/intentionally-not-mapped") {
      status should equal (404)
      body should equal ("notFound block executed")
    }
  }
}

