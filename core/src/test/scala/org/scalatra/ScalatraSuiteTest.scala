package org.scalatra

import test.scalatest.ScalatraFunSuite

class ScalatraSuiteTestServlet extends ScalatraServlet {
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
    request.getMethod
  }

  put("/method") {
    request.getMethod
  }

  delete("/method") {
    request.getMethod
  }
}

class ScalatraSuiteTest extends ScalatraFunSuite {
  addServlet(classOf[ScalatraSuiteTestServlet], "/*")

  test("route test") {
    get("/") {
      status should equal(200)
      body should include("root")
    }
  }

  test("get test") {
    get("/echo_params", "msg" -> "hi") {
      status should equal(200)
      body should equal("hi")
    }
  }
  test("get with multi-byte chars in params") {
    // `msg` will automatically be url-encoded by ScalatraSuite
    get("/echo_params", "msg" -> "こんにちわ") {
      status should equal(200)
      body should equal("こんにちわ")
    }
  }

  test("post test") {
    post("/echo_params", "msg" -> "hi") {
      status should equal(200)
      body should equal("hi")
    }
  }

  test("post multi-byte chars test") {
    // `msg` will automatically be url-encoded by ScalatraSuite
    post("/echo_params", "msg" -> "こんにちわ") {
      status should equal(200)
      body should equal("こんにちわ")
    }
  }

  test("header test") {
    get("/redirect") {
      status should equal(302)
      header("Location") should include("/redirected")
    }
  }

  test("abbrevs test") {
    get("/redirect") {
      status should equal(response status)
      body should equal(response body)
      header("Location") should equal(response header ("Location"))
    }
  }

  test("session test") {
    val name = "Scalatra"
    post("/session", "name" -> name) {
      status should equal(200)
      body should include(name)
    }
    get("/session") {
      status should equal(200)
      body should not include (name)
    }
    session {
      post("/session", "name" -> name) {
        status should equal(200)
        body should include(name)
      }
      get("/session") {
        status should equal(200)
        body should include(name)
      }
      get("/session") {
        status should equal(200)
        body should include(name)
      }
    }
  }

  test("put test") {
    put("/method") {
      body should equal("PUT")
    }
  }

  test("delete test") {
    delete("/method") {
      body should equal("DELETE")
    }
  }

  test("patch test") {
    patch("/method") {
      body should equal("PATCH")
    }
  }
}
