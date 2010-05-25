package com.thinkminimo.step

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import io.Source
import org.mortbay.jetty.testing.{ServletTester, HttpTester}

class RequestBodyTestServlet extends Step {
  get("/request-body") {
    Source.fromInputStream(request.getInputStream).getLines().mkString
  }

  get("/request-body/:method") {
    request.body + params(":method")
  }
}

class RequestBodyTest extends FunSuite with ShouldMatchers {
  val tester = new ServletTester
  tester.addServlet(classOf[RequestBodyTestServlet], "/")
  tester.start
  
  test("can read request body") {
    /*
     * A user on the mailing list is having trouble reading the request body.  Make sure that it works.
     */
    val req = new HttpTester
    req.setVersion("HTTP/1.0")
    req.setMethod("GET")
    req.setURI("/request-body")
    req.setContent("My cat's breath smells like cat food!")
    val res = new HttpTester
    res.parse(tester.getResponses(req.generate))
    res.getContent should equal ("My cat's breath smells like cat food!")
  }

  test("can read request body with the body-method") {
    val req = new HttpTester
    req.setVersion("HTTP/1.0")
    req.setMethod("GET")
    req.setURI("/request-body/fish!")
    req.setContent("My cat's breath smells like ")
    val res = new HttpTester
    res.parse(tester.getResponses(req.generate))
    res.getContent should equal ("My cat's breath smells like fish!")
  }
}