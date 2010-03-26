package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers
import javax.servlet.http.HttpServletResponse

class HaltTestServlet extends Step {
  get("/halts-response") {
    response.setHeader("testHeader", "testHeader")
    halt(501, "Not implemented (for test)")
    "this content must not be returned"
  }

  get("/halt-no-message") {
    halt(418) // I'm a teapot
    "This response MAY be short and stout."
  }

  get("/halt-no-status") {
    status(HttpServletResponse.SC_ACCEPTED)
    halt()
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

  test("halt without a message halts") {
    get("/halt-no-message") {
      status should equal(418)
      body should not include "short and stout"
    }
  }

  test("halt without status halts without affecting the status") {
    get("/halt-no-status") {
      status should equal (HttpServletResponse.SC_ACCEPTED)
      body should not include ("this content must not be returned")
    }
  }
}