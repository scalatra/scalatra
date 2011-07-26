package org.scalatra

import javax.servlet.http.HttpServletResponse
import test.scalatest.ScalatraFunSuite

class BeforeTestServlet extends ScalatraServlet {
  
  beforeAll {
    response.setStatus(204)
  }
  
  beforeSome("/some/path") {
    response.setStatus(202)
  }
  
  beforeSome("/other/path") {
    response.setStatus(206)
  }
  
  get("/some/path") { }
  
  get("/other/path") { }
  
  get("/third/path") { }
  
}

class BeforeTest extends ScalatraFunSuite {
  addServlet(classOf[BeforeTestServlet], "/*")
  
  test("beforeAll is applied to all paths") {
    get("/third/path") {
      status should equal(204)
    }
  }
  
  test("before only applies to a given path") {
    get("/some/path") {
      status should equal(202)
    }
    get("/other/path") {
      status should equal(206)
    }
  }
  
}
