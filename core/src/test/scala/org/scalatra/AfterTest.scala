package org.scalatra

import javax.servlet.http.HttpServletResponse
import test.scalatest.ScalatraFunSuite

class AfterTestServlet extends ScalatraServlet {
  
  after() {
    response.setStatus(204)
  }
  
  after("/some/path") {
    response.setStatus(202)
  }
  
  after("/other/path") {
    response.setStatus(206)
  }
  
  get("/some/path") { }
  
  get("/other/path") { }
  
  get("/third/path") { }
  
}

class AfterTest extends ScalatraFunSuite {
  addServlet(classOf[AfterTestServlet], "/*")
  
  test("afterAll is applied to all paths") {
    get("/third/path") {
      status should equal(204)
    }
  }
  
  test("after only applies to a given path") {
    get("/some/path") {
      status should equal(202)
    }
    get("/other/path") {
      status should equal(206)
    }
  }
  
}
