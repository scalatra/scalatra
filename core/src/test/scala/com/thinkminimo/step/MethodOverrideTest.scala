package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class MethodOverrideTestServlet extends Step with MethodOverride {
  put("/foo") { "PUT" }
  post("/foo") { "POST" }
  get("/foo") { "GET" }
}

class MethodOverrideTest extends StepSuite with ShouldMatchers {
  route(classOf[MethodOverrideTestServlet], "/*")

  test("should override method with _method parameter on post") {
    post("/foo", "_method" -> "put") {
      body should equal ("PUT")
    }
  }

  test("should not override method if _method parameter is not set") {
    post("/foo") {
      body should equal ("POST")
    }
  }

  test("should not override method for methods other than POST") {
    get("/foo", "_method" -> "put") {
      body should equal ("GET")
    }
  }
}

