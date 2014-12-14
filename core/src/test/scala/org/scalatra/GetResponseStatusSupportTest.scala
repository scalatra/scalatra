package org.scalatra

import test.scalatest.ScalatraFunSuite

class GetResponseStatusSupportTestServlet extends ScalatraServlet {
  before() {
    session // Establish a session before we commit the response
  }

  after() {
    session("status") = status.toString
  }

  get("/status/:status") {
    response.setStatus(params("status").toInt)
    status.toString
  }

  get("/redirect") {
    response.sendRedirect("/")
  }

  get("/session-status") {
    session.getOrElse("status", "none")
  }

  get("/send-error/:status") {
    response.sendError(params("status").toInt)
  }

  get("/send-error/:status/:msg") {
    response.sendError(params("status").toInt, params("msg"))
  }
}

class GetResponseStatusSupportTest extends ScalatraFunSuite {
  addServlet(classOf[GetResponseStatusSupportTestServlet], "/*")

  test("remember status after setStatus") {
    get("/status/404") {
      body should equal("404")
    }
  }

  test("remembers status after sendRedirect") {
    session {
      get("/redirect") {}
      get("/session-status") { body should equal("302") }
    }
  }

  test("remembers status after sendError without a message") {
    session {
      get("/send-error/500") {}
      get("/session-status") { body should equal("500") }
    }
  }

  test("remembers status after sendError with a message") {
    session {
      get("/send-error/504/Gateway%20Timeout") {}
      get("/session-status") { body should equal("504") }
    }
  }
}
