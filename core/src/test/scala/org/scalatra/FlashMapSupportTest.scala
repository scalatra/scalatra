package org.scalatra

import javax.servlet.http.HttpServletRequest

import org.scalatra.test.scalatest.ScalatraFunSuite

class FlashMapSupportTestServlet extends ScalatraServlet with FlashMapSupport {
  post("/message") {
    flash("message") = "posted"
  }

  get("/message") {
    flash.get("message") foreach { x => response.setHeader("message", x.toString) }
  }

  post("/commit") {
    flash("message") = "oops"
    response.flushBuffer // commit response
  }

  get("/unused") {}

  override def sweepUnusedFlashEntries(req: HttpServletRequest) = {
    req.getParameter("sweep") match {
      case null => false
      case x => x.toBoolean
    }
  }
}

class FlashMapSupportSecondTestServlet extends ScalatraServlet with FlashMapSupport {
  post("/redirect") {
    flash("message") = "redirected"
    redirect("/first/message")
  }
}

class FlashMapSupportTestFilter extends ScalatraFilter with FlashMapSupport {
  get("/filter") {
    flash.get("message") foreach { x => response.setHeader("message", x.toString) }
  }

  override def sweepUnusedFlashEntries(req: HttpServletRequest) = req.getParameter("sweep") match {
    case null => false
    case x => x.toBoolean
  }
}

class FlashMapSupportTest extends ScalatraFunSuite {
  addFilter(classOf[FlashMapSupportTestFilter], "/*")
  addServlet(classOf[FlashMapSupportTestServlet], "/*")

  test("should sweep flash map at end of request") {
    session {
      post("/message") {}

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

  test("messages should be available in outer filter when flash map supports are nested") {
    session {
      post("/message") {}
      get("/filter") {
        header("message") should equal("posted")
      }
    }
  }

  test("does not sweep unused entries if flag is false") {
    session {
      post("/message") {}

      get("/unused", "sweep" -> "false") {}

      get("/message") {
        header("message") should equal("posted")
      }
    }
  }

  test("sweeps unused entries if flag is true") {
    println("TEST: sweeps unused entries if flag is true")
    session {
      post("/message") {}

      get("/unused", "sweep" -> "true") {}

      get("/message") {
        header("message") should equal(null)
      }
    }
  }
}

// Based on issue #57
class FlashMapSupportTwoServletsTest extends ScalatraFunSuite {
  addServlet(classOf[FlashMapSupportTestServlet], "/first/*")
  addServlet(classOf[FlashMapSupportSecondTestServlet], "/second/*")

  test("should clear message when displayed in other servlet") {
    session {
      post("/second/redirect") {}
      get("/first/message") {
        header("message") should equal("redirected")
      }
      get("/first/message") {
        header("message") should equal(null)
      }
    }
  }

  test("works if message page is hit first") {
    session {
      get("/first/message") {}
      post("/second/redirect") {}
      get("/first/message") {
        header("message") should equal("redirected")
      }
    }
  }
}
