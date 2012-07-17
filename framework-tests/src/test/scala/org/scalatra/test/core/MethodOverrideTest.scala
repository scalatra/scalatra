package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

class MethodOverrideTestApp extends ScalatraApp with MethodOverride {



  put("/foo") { "PUT" }
  post("/foo") { "POST" }
  get("/foo") { "GET" }

  /**
   * Returns a request identical to the current request, but with the
   * specified method.
   *
   * For backward compatibility, we need to transform the underlying request
   * type to pass to the super handler.
   */
  protected def requestWithMethod(req: HttpRequest, method: HttpMethod): HttpRequest = req
}

abstract class MethodOverrideTest extends ScalatraFunSuite {
  mount(new MethodOverrideTestApp)

  test("should override method with _method parameter on post") {
    post("/foo", MethodOverride.ParamName -> "put") {
      body should equal ("PUT")
    }
  }

  test("should not override method if _method parameter is not set") {
    post("/foo") {
      body should equal ("POST")
    }
  }

  test("should not override method for methods other than POST") {
    get("/foo", MethodOverride.ParamName -> "put") {
      body should equal ("GET")
    }
  }

  test("should override method with X-HTTP-METHOD-OVERRIDE header on post") {
    post("/foo", headers = Map(MethodOverride.HeaderName -> "put")) {
      body should equal ("PUT")
    }
  }

  test("should not override method if X-HTTP-METHOD-OVERRIDE is not set") {
    post("/foo") {
      body should equal ("POST")
    }
  }

  test("should not override method for methods other than POST with X-HTTP-METHOD-OVERRIDE header") {
    get("/foo", headers = Map(MethodOverride.HeaderName -> "put")) {
      body should equal ("GET")
    }
  }
}

class NettyMethodOverrideTest extends MethodOverrideTest with NettyBackend
class JettyMethodOverrideTest extends MethodOverrideTest with JettyBackend


