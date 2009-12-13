package com.thinkminimo.step

import javax.servlet.http._
import org.mortbay.jetty.testing.HttpTester
import org.mortbay.jetty.testing.ServletTester
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

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

  get("/session") {
    session("val") match {
      case Some(v:String) => v
      case _ => "None"
    }
  }

  post("/session") {
    session("val") = params("val")
    session("val") match {
      case Some(v:String) => v
      case _ => "None"
    }
  }

  get("/no_content") {
    status(204)
    ""
  }
}

class StepSuite extends FunSuite with ShouldMatchers {

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

  test("POST /post/something/val with posted_value=yes should return 'yessomething'") {
    request.setMethod("POST")
    request.setURI("/post/something/val")
    request.addHeader("Content-Type", "application/x-www-form-urlencoded")
    request.setContent("posted_value=yes")
    response.parse(tester.getResponses(request.generate()))
    assert(response.getContent === "yessomething")
  }

  test("GET /session with no session should return 'None'") {
    request.setMethod("GET")
    request.setURI("/session")
    response.parse(tester.getResponses(request.generate))
    assert(response.getContent === "None")
  }

  test("POST /session with val=yes should return 'yes'") {
    request.setMethod("POST")
    request.setURI("/session")
    request.addHeader("Content-Type", "application/x-www-form-urlencoded")
    request.setContent("val=yes")
    response.parse(tester.getResponses(request.generate))
    assert(response.getContent === "yes")
  }

  test("GET /session with the session should return the data set in POST /session") {
    val data = "session_value"

    request.setMethod("POST")
    request.setURI("/session")
    request.addHeader("Content-Type", "application/x-www-form-urlencoded")
    request.setContent("val=" + data)
    response.parse(tester.getResponses(request.generate))
    assert(response.getContent === data)

    // keep the previous session and check if the data remains
    request.setMethod("GET")
    request.setURI("/session")
    request.setHeader(
      "Cookie",
      response.getHeaderValues("Set-Cookie").nextElement.toString
    )
    response.parse(tester.getResponses(request.generate))
    assert(response.getContent === data)
  }

  test("GET /no_content should return 204(HttpServletResponse.SC_NO_CONTENT)") {
    request.setMethod("GET")
    request.setURI("/no_content")
    response.parse(tester.getResponses(request.generate))
    response.getStatus should equal (204)
  }
}
