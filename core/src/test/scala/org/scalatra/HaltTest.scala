package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import javax.servlet.http.HttpServletResponse
import test.scalatest.ScalatraFunSuite

class HaltTestServlet extends ScalatraServlet {
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

  before {
    if (params.isDefinedAt("haltBefore")) {
      halt(503)
    }
  }

  after {
    response.setHeader("After-Block-Ran", "true")
  }
}

class HaltTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(classOf[HaltTestServlet], "/*")

  test("GET /halts-response halts processing of the action") {
    get("/halts-response") {
      status should equal(501)
      body should not include ("this content must not be returned")
      body should include ("Not implemented (for test)")
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

  test("can halt out of begin filter") {
    get("/halts-response", "haltBefore" -> "true") {
      status should equal(503)
    }
  }

  test("after filter runs after halt") {
    /*
     * This behavior is not entirely intuitive, but it's what Sinatra does.  If you're using your after filters for
     * logging, then it's a good thing.  If you're using your after filters to modify the response, then this might
     * bite you.
     */
    get("/halt-no-status") {
      response.getHeader("After-Block-Ran") should equal ("true")
    }
  }
}