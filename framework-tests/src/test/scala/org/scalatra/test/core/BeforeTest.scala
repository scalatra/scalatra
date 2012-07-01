package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

class BeforeTestApp extends ScalatraApp {

  before() {
    status = 204
  }

  before("/some/path") {
    status = 202
  }

  before("/other/path") {
    status = 206
  }

  get("/some/path") { }

  get("/other/path") { }

  get("/third/path") { }

}

abstract class BeforeTest extends ScalatraFunSuite {
  mount(new BeforeTestApp)

  test("beforeAll is applied to all paths") {
    get("/third/path") {
      status.code should equal(204)
    }
  }

  test("before only applies to a given path") {
    get("/some/path") {
      status.code should equal(202)
    }
    get("/other/path") {
      status.code should equal(206)
    }
  }

}

class NettyBeforeTest extends BeforeTest with NettyBackend
class JettyBeforeTest extends BeforeTest with JettyBackend
