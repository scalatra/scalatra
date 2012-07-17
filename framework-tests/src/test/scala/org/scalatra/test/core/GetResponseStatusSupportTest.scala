package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

class GetResponseStatusSupportTestApp extends ScalatraApp with SessionSupport {
  before() {
    session // Establish a session before we commit the response
  }

  after() {
    session("status") = status.toString
  }

  get("/status/:status") {
    response.status = ResponseStatus(params("status").toInt)
    status.toString
  }

  get("/redirect") {
    response.redirect("/")
  }

  get("/session-status") {
    session.getOrElse("status", "none")
  }

  get("/send-error/:status") {
    halt(params("status").toInt)
  }

  get("/send-error/:status/:msg") {
    halt(params("status").toInt, params("msg"))
  }
}

abstract class GetResponseStatusSupportTest extends ScalatraFunSuite {
  mount(new GetResponseStatusSupportTestApp)

  test("remember status after setStatus") {
    get("/status/404") {
      body should equal ("404")
    }
  }

  test("remembers status after sendRedirect") {
    session {
      get("/redirect") {}
      get("/session-status") { body should equal ("302") }
    }
  }

  test("remembers status after sendError without a message") {
    session {
      get("/send-error/500") {}
      get("/session-status") { body should equal ("500") }
    }
  }

  test("remembers status after sendError with a message") {
    session {
      get("/send-error/504/Gateway%20Timeout") {}
      get("/session-status") { body should equal ("504") }
    }
  }
}

class NettyGetResponseStatusSupportTest extends GetResponseStatusSupportTest with NettyBackend
class JettyGetResponseStatusSupportTest extends GetResponseStatusSupportTest with JettyBackend