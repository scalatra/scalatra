package org.scalatra

import test.NettyBackend
import test.scalatest.ScalatraFunSuite

class AfterTestApp extends ScalatraApp {

  after() {
    status = 204
  }

  after("/some/path") {
    status = 202
  }

  after("/other/path") {
    status = 206
  }

  get("/some/path") { }

  get("/other/path") { }

  get("/third/path") { }

}

abstract class AfterTest extends ScalatraFunSuite {
  mount(new AfterTestApp)

  test("afterAll is applied to all paths") {
    get("/third/path") {
      status.code should equal(204)
    }
  }

  test("after only applies to a given path") {
    get("/some/path") {
      status.code should equal(202)
    }
    get("/other/path") {
      status.code should equal(206)
    }
  }

}

class NettyAfterTest extends AfterTest with NettyBackend
