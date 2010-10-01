package org.scalatra

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

class RequestBodyTestServlet extends ScalatraServlet {
  post("/request-body") {
    request.body
  }
}

@RunWith(classOf[JUnitRunner])
class RequestBodyTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(classOf[RequestBodyTestServlet], "/")

  test("can read request body") {
    post("/request-body", "My cat's breath smells like cat food!") {
      body should equal ("My cat's breath smells like cat food!")
    }
  }
}
