package org.scalatra

import org.scalatra.test.scalatest.ScalatraFunSuite

class MethodOverrideTestServlet extends ScalatraServlet with MethodOverride {
  put("/foo") { "PUT" }
  post("/foo") { "POST" }
  get("/foo") { "GET" }
}

class MethodOverrideTest extends ScalatraFunSuite {
  addServlet(classOf[MethodOverrideTestServlet], "/*")

  test("should override method with _method parameter on post") {
    post("/foo", MethodOverride.ParamName -> "put") {
      body should equal("PUT")
    }
  }

  test("should not override method if _method parameter is not set") {
    post("/foo") {
      body should equal("POST")
    }
  }

  test("should not override method for methods other than POST") {
    get("/foo", MethodOverride.ParamName -> "put") {
      body should equal("GET")
    }
  }

  test("should override method with X-HTTP-METHOD-OVERRIDE header on post") {
    post("/foo", headers = Map(MethodOverride.HeaderName.toList(0) -> "put")) {
      body should equal("PUT")
    }
  }

  test("should not override method if X-HTTP-METHOD-OVERRIDE is not set") {
    post("/foo") {
      body should equal("POST")
    }
  }

  test("should not override method for methods other than POST with X-HTTP-METHOD header") {
    get("/foo", headers = Map(MethodOverride.HeaderName.toList(1) -> "put")) {
      body should equal("GET")
    }
  }
}

