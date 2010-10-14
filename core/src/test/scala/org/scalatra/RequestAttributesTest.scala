package org.scalatra

import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

class RequestAttributesTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(new ScalatraServlet {
    get("/request") {
      request.attributes("one") = "1"
      request.attributes("two") = "2"
      request.attributes("three") = "3"
      request.attributes -= "two"
      request.attributes foreach { case(k, v) => response.setHeader(k, v.toString) }
    }
  }, "/*")

  test("request.attributes.apply should set request attribute") {
    get("/request") {
      header("one") should equal ("1")
      header("three") should equal ("3")
    }
  }

  test("request.attributes.-= should remove request attribute") {
    get("/request") {
      header("two") should equal (null)
    }
  }
}

