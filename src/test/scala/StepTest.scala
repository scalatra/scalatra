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

  get("/redirect") {
    redirect("/redirected")
  }

  get("/redirected") {
    "redirected"
  }

  get("/print_referer") {
    request referer
  }

  get("/print_host") {
    "host:" + request.host + ",port:" + request.port
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

  test("GET /redirect redirects to /redirected") {
    request.setMethod("GET")
    request.setURI("/redirect")
    response.parse(tester.getResponses(request.generate))
    assert(response.getHeader("Location") endsWith "/redirected")
  }

  test("POST /post/test with posted_value=<multi-byte str> should return the multi-byte str") {
    val urlEncodedMultiByteStr = java.net.URLEncoder.encode("こんにちは", "UTF-8")

    request.setMethod("POST")
    request.setURI("/post/test")
    request.addHeader("Content-Type", "application/x-www-form-urlencoded")
    request.setContent("posted_value=" + urlEncodedMultiByteStr)
    // The generated response should be decoded with UTF-8,
    // but the HttpTester always try to decode it with ISO_8859-1, causing an error.
    // Here, we have to transform the response from UTF-8 to ISO_8859-1 managing HttpTester.parse to work.
    response.parse(new String(tester.getResponses(request.generate()).getBytes("UTF-8"), "ISO_8859-1"))
    assert(response.getContent === "こんにちは")
  }

  test("GET /print_referer should return Referer") {
    request.setMethod("GET")
    request.setURI("/print_referer")
    request.setHeader("Referer", "somewhere")
    request.setContent("")
    response.parse(tester.getResponses(request.generate))
    response.getContent should equal ("somewhere")
  }

  test("GET /print_host should return the host's name/port") {
    request.setVersion("HTTP/1.0")
    request.setMethod("GET")
    request.setURI("/print_host")
    request.setHeader("Host", "localhost:80")
    response.parse(tester.getResponses(request.generate))
    response.getContent should equal ("host:localhost,port:80")

    request.setHeader("Host", "hoge.com:1234")
    response.parse(tester.getResponses(request.generate))
    response.getContent should equal ("host:hoge.com,port:1234")
    
    request.setHeader("Host", "1.2.3.4")
    response.parse(tester.getResponses(request.generate))
    response.getContent should equal ("host:1.2.3.4,port:")
  }
}
