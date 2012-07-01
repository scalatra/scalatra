package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

abstract class StatusCodeHandlerTest extends ScalatraFunSuite {

  trait TestException extends RuntimeException

  case class Exception1() extends TestException

  case class Exception2() extends TestException

  class BaseApp extends ScalatraApp {
    get("/401") {
      status = 401
    }
    get("/402") {
      status = 402
    }

    get("/500") {
      status = 500
    }

    trap(400 to 402) {
      status = 200
      "400s"
    }

    trap(500) {
      "internal error"
    }
  }

  class ChildApp extends BaseApp {
    get("/500_2") {
      status = 500
      "discarded"
    }

    get("/400") {
      status = 400
    }

    trap(500) {
      status = 303
      "child error"
    }

  }

  mount("/base", new BaseApp)
  mount("/child", new ChildApp)

  test("status code 401 should be trapped by range handler") {
    get("/base/401") {
      status.code should equal(200)
      body should equal("400s")
    }
  }

  test("status code 402 should be trapped by range handler") {
    get("/base/402") {
      status.code should equal(200)
      body should equal("400s")
    }
  }

  test("status code 500 should be trapped by single handler") {
    get("/base/500") {
      body should equal("internal error")
    }
  }

  test("status handlers are composable") {
    get("/child/500_2") {
      body should equal("child error")
      status.code should equal (303)
    }

    get("/child/400") {
      status.code should equal (200)
      body should equal("400s")
    }
  }
}

class NettyStatusCodeHandlerTest extends StatusCodeHandlerTest with NettyBackend
class JettyStatusCodeHandlerTest extends StatusCodeHandlerTest with JettyBackend
