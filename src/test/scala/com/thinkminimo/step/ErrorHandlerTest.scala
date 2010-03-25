package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class ErrorHandlerTestServlet extends Step {
  get("/") {
    throw new RuntimeException
  }

  error {
    "handled " + caughtException.getClass.getName
  }
}

class ErrorHandlerTest extends StepSuite with ShouldMatchers {
  route(classOf[ErrorHandlerTestServlet], "/*")

  test("result of error handler should be rendered") {
    get("/") {
      body should equal ("handled java.lang.RuntimeException")
    }
  }

  test("response status should be set to 500 on error") {
    get("/") {
      status should equal (500)
    }
  }
}