package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import javax.servlet.http.HttpServletResponse
import test.scalatest.ScalatraFunSuite

class BeforeTestServlet extends ScalatraServlet {
  
  before {
    response.setStatus(204)
  }
  
  before("/some/path") {
    response.setStatus(202)
  }
  
  before("/other/path") {
    response.setStatus(206)
  }
  
  get("/some/path") { }
  
  get("/other/path") { }
  
  get("/third/path") { }
  
}

class BeforeTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(classOf[BeforeTestServlet], "/*")
  
  test("before without path is applied to all paths") {
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