package org.scalatra

import test.scalatest.ScalatraFunSuite

class StatusCodeHandlerTest extends ScalatraFunSuite {

  trait TestException extends RuntimeException

  case class Exception1() extends TestException

  case class Exception2() extends TestException

  class BaseServlet extends ScalatraServlet {
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

  class ChildServlet extends BaseServlet {
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

  addServlet(new BaseServlet, "/base/*")
  addServlet(new ChildServlet, "/child/*")

  test("status code 401 should be trapped by range handler") {
    get("/base/401") {
      status should equal(200)
      body should equal("400s")
    }
  }

  test("status code 402 should be trapped by range handler") {
    get("/base/402") {
      status should equal(200)
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
      status should equal (303)
    }

    get("/child/400") {
      status should equal (200)
      body should equal("400s")
    }
  }
}
