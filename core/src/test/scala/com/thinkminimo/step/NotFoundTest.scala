package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class DefaultNotFoundTestServlet extends Step
class CustomNotFoundTestServlet extends Step {
  notFound {
    response.setStatus(404)
    "notFound block executed"
  }
}

class NotFoundTest extends StepSuite with ShouldMatchers {
  route(classOf[DefaultNotFoundTestServlet], "/default/*")
  route(classOf[CustomNotFoundTestServlet], "/custom/*")
  
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

