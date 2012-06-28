package org.scalatra

import test.NettyBackend
import test.scalatest.ScalatraFunSuite

class ScalatraSuiteTestServlet extends ScalatraApp with SessionSupport {
  before() {
    contentType = "text/html; charset=utf-8"
  }

  get("/") {
    "root"
  }

  get("/session") {
    session.getOrElse("name", "error!")
  }

  post("/session") {
    session("name") = params("name")
    session.getOrElse("name", "error!")
  }

  get("/redirect") {
    redirect("/redirected")
  }

  get("/echo_params") {
    params("msg")
  }

  post("/echo_params") {
    params("msg")
  }

  patch("/method") {
    request.requestMethod
  }

  put("/method") {
    request.requestMethod
  }

  delete("/method") {
    request.requestMethod
  }
}

abstract class ScalatraSuiteTest extends ScalatraFunSuite {
  mount(new ScalatraSuiteTestServlet)

  test("route test") {
    get("/") {
      status.code should equal (200)
      body should include ("root")
    }
  }

  test("get test") {
    get("/echo_params", "msg" -> "hi") {
      status.code should equal (200)
      body should equal ("hi")
    }
  }
  test("get with multi-byte chars in params") {
    // `msg` will automatically be url-encoded by ScalatraSuite
    get("/echo_params", "msg" -> "こんにちわ") {
      status.code should equal (200)
      body should equal ("こんにちわ")
    }
  }

  test("post test") {
    post("/echo_params", "msg" -> "hi") {
      status.code should equal (200)
      body should equal ("hi")
    }
  }

  test("post multi-byte chars test") {
    // `msg` will automatically be url-encoded by ScalatraSuite
    post("/echo_params", "msg" -> "こんにちわ") {
      status.code should equal (200)
      body should equal ("こんにちわ")
    }
  }

  test("header test") {
    get("/redirect") {
      status.code should equal (302)
      headers("Location") should include ("/redirected")
    }
  }

  test("abbrevs test") {
    get("/redirect") {
      status.code should equal (302)
      body should equal (response body)
      headers("Location") should equal (response headers("Location"))
    }
  }

  test("session test") {
    val name = "Scalatra"
    post("/session", "name" -> name) {
      status.code should equal (200)
      body should include (name)
    }
    get("/session") {
      status.code should equal (200)
      body should not include (name)
    }
    session {
      post("/session", "name" -> name) {
        status.code should equal (200)
        body should include (name)
      }
      get("/session") {
        status.code should equal (200)
        body should include (name)
      }
      get("/session") {
        status.code should equal (200)
        body should include (name)
      }
    }
  }

  test("put test") {
    put("/method") {
      body should equal ("PUT")
    }
  }

  test("delete test") {
    deleteReq("/method") {
      body should equal ("DELETE")
    }
  }

  test("patch test") {
    patch("/method") {
      body should equal ("PATCH")
    }
  }
}

class NettyScalatraSuiteTest extends ScalatraSuiteTest with NettyBackend