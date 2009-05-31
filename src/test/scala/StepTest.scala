package com.thinkminimo.step

import javax.servlet.http._
import org.mortbay.jetty.testing.HttpTester
import org.mortbay.jetty.testing.ServletTester
import org.scalatest.FunSuite

class TestServlet extends Step {
  get("/") {
    "root"
  }

  get("/this/:test/should/:pass") {
    params(":test")+params(":pass")
  }

  get("/xml/:must/:val") {
    <h1>{ params(":must")+params(":val") }</h1>
  }

  get("/number") {
    42
  }

  post("/post/test") {
    params("posted_value")
  }

  post("/post/:test/val") {
    params("posted_value")+params(":test")
  }
}

class StepSuite extends FunSuite {

  val tester = new ServletTester()
  val response = new HttpTester()
  val request = new HttpTester()
  request.setVersion("HTTP/1.0")
  tester.addServlet(classOf[TestServlet], "/*")
  tester.start()

  test("GET / should return 'root'") {
    request.setMethod("GET")
    request.setURI("/")
    response.parse(tester.getResponses(request.generate()))
    assert(response.getContent === "root")
  }

  test("GET /this/will/should/work should return 'willwork'") {
    request.setMethod("GET")
    request.setURI("/this/will/should/work")
    response.parse(tester.getResponses(request.generate()))
    assert(response.getContent === "willwork")
  }

  test("GET /xml/really/works should return '<h1>reallyworks</h1>'") {
    request.setMethod("GET")
    request.setURI("/xml/really/works")
    response.parse(tester.getResponses(request.generate()))
    assert(response.getContent === "<h1>reallyworks</h1>")
  }

  test("GET /number should return '42'") {
    request.setMethod("GET")
    request.setURI("/number")
    response.parse(tester.getResponses(request.generate()))
    assert(response.getContent === "42")
  }

  test("POST /post/test with posted_value=yes should return 'yes'") {
    request.setMethod("POST")
    request.setURI("/post/test")
    request.addHeader("Content-Type", "application/x-www-form-urlencoded")
    request.setContent("posted_value=yes")
    response.parse(tester.getResponses(request.generate()))
    assert(response.getContent === "yes")
  }
}
