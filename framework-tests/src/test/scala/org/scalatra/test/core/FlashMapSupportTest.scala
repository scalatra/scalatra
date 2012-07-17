package org.scalatra


import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

class FlashMapSupportTestServlet extends ScalatraApp with SessionSupport with FlashMapSupport {
  post("/message") {
    flash("message") = "posted"
  }

  get("/message") {
    flash.get("message") foreach { x => response.headers += "message" -> x.toString }
  }

  post("/commit") {
    flash("message") = "oops"
    response.end() // commit response
  }

  get("/unused") {}

  override def sweepUnusedFlashEntries(req: HttpRequest) = {
    val r = req.parameters.get("sweep").map(_.toBoolean).getOrElse(false)
    printf("should sweep: %b, parameter: %s\n", r, req.parameters.get("sweep"))
    r
  }
}

class FlashMapSupportSecondTestServlet extends ScalatraApp with SessionSupport with FlashMapSupport {
  post("/redirect") {
    flash("message") = "redirected"
    redirect("/first/message")
  }
}

//class FlashMapSupportTestFilter extends ScalatraApp with SessionSupport with FlashMapSupport {
//  get("/filter") {
//    flash.get("message") foreach { x => response.setHeader("message", x.toString) }
//  }
//}

abstract class FlashMapSupportTest extends ScalatraFunSuite {
//  addFilter(classOf[FlashMapSupportTestFilter], "/*")
  mount(new FlashMapSupportTestServlet)
  test("should sweep flash map at end of request") {
    session {
      post("/message") {}

      get("/message") {
        headers("message") should equal("posted")
      }

      get("/message") {
        headers.get("message") should equal(None)
      }
    }
  }

  test("should sweep flash map even if response has been committed") {
    session {
      post("/commit") {}

      get("/message") {
        headers("message") should equal("oops")
      }
    }
  }

  test("flash map is session-scoped") {
    post("/message") {}

    get("/message") {
      headers.get("message") should equal(None)
    }
  }
//
//  test("messages should be available in outer filter when flash map supports are nested") {
//    session {
//      post("/message") {}
//      get("/filter") {
//        headers("message") should equal ("posted")
//      }
//    }
//  }

  test("does not sweep unused entries if flag is false") {
    session {
      post("/message") {}

      get("/unused", "sweep" -> "false") {}

      get("/message") {
        headers("message") should equal ("posted")
      }
    }
  }

  test("sweeps unused entries if flag is true") {
    session {
      post("/message") {}

      get("/unused", "sweep" -> "true") {}

      get("/message") {
        headers.get("message") should equal (None) //(Some("posted"))
      }
    }
  }
}

class NettyFlashMapSupportTest extends FlashMapSupportTest with NettyBackend

// Based on issue #57
abstract class FlashMapSupportTwoServletsTest extends ScalatraFunSuite {
  mount("/first", new FlashMapSupportTestServlet)
  mount("/second", new FlashMapSupportSecondTestServlet)

  test("should clear message when displayed in other servlet") {
    session {
      post("/second/redirect") {}
      get("/first/message") {
        headers("message") should equal ("redirected")
      }
      get("/first/message") {
        headers.get("message") should equal (None)
      }
    }
  }

  test("works if message page is hit first") {
    session {
      get("/first/message") {}
      post("/second/redirect") {}
      get("/first/message") {
        headers("message") should equal ("redirected")
      }
    }
  }
}

class NettyFlashMapSupportTwoServletsTest extends FlashMapSupportTwoServletsTest with NettyBackend
class JettyFlashMapSupportTwoServletsTest extends FlashMapSupportTwoServletsTest with JettyBackend