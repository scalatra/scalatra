package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

abstract class ErrorHandlerTest extends ScalatraFunSuite {
  trait TestException extends RuntimeException
  case class Exception1() extends TestException
  case class Exception2() extends TestException
  case class Exception3() extends TestException

  class BaseApp extends ScalatraApp {
    get("/1") {
      status = 418
      throw new Exception1
    }
    get("/uncaught") { throw new RuntimeException }
    error { case e: TestException => "base" }
  }

  class ChildApp extends BaseApp {
    get("/2") { throw new Exception2 }
    error { case e: Exception2 => "child" }
  }

  class HaltApp extends BaseApp {
    get("/3") { throw new Exception3 }
    error { case e: Exception3 => halt(413, "no more") }
  }

  mount("/base", new BaseApp)
  mount("/child", new ChildApp)
  mount("/halt", new HaltApp)

  test("result of error handler should be rendered") {
    get("/base/1") {
      body should equal ("base")
    }
  }

  test("error handlers are composable") {
    get("/child/2") {
      body should equal ("child")
    }

    get("/child/1") {
      body should equal ("base")
    }
  }

  test("response status should not be set on error") {
    get("/base/1") {
      status.code should equal (418)
    }
  }

  test("rethrows uncaught exceptions") {
    get("/base/uncaught") {
      status.code should equal (500)
    }
  }

  test("halt() can be used from error handler") {
    get("/halt/3") {
      status.code should equal (413)
      body should equal ("no more")
    }
  }
}

class NettyErrorHandlerTest extends ErrorHandlerTest with NettyBackend
class JettyErrorHandlerTest extends ErrorHandlerTest with JettyBackend