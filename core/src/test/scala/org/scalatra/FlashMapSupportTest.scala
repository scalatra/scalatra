package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

class FlashMapSupportTestServlet extends ScalatraServlet with FlashMapSupport {
  post("/message") {
    flash("message") = "posted"
    flash.get("message") foreach { x => response.setHeader("message", x.toString) }
  }

  get("/message") {
    flash.get("message") foreach { x => response.setHeader("message", x.toString) }
  }

  post("/commit") {
    flash("message") = "oops"
    response.flushBuffer // commit response
  }
}

class FlashMapSupportTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(classOf[FlashMapSupportTestServlet], "/*")

  test("should sweep flash map at end of request") {
    session {
      post("/message") {
        header("message") should equal(null)
      }

      get("/message") {
        header("message") should equal("posted")
      }

      get("/message") {
        header("message") should equal(null)
      }
    }
  }

  test("should sweep flash map even if response has been committed") {
    session {
      post("/commit") {}

      get("/message") {
        header("message") should equal("oops")
      }
    }
  }

  test("flash map is session-scoped") {
    post("/message") {}

    get("/message") {
      header("message") should equal(null)
    }
  }
}

