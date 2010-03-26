package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class HaltTestServlet extends Step {
  get("/halts-response") {
    response.setHeader("testHeader", "testHeader")
    halt(501, "Not implemented (for test)")
    "this content must not be returned"
  }
}

class HaltTest extends StepSuite with ShouldMatchers {
  route(classOf[HaltTestServlet], "/*")

  test("GET /halts-response halts processing of the action") {
    get("/halts-response") {
      status should equal(501)
      body should not equal("this content must not be returned")
      body.contains("Not implemented (for test)")
    }
  }

  test("GET /halts-response - halt doesn't clear headers") {
    get("/halts-response") {
      response.getHeader("testHeader") should equal("testHeader")
    }
  }
}