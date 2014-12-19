package org.scalatra

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import test.scalatest.ScalatraFunSuite

class RequestBodyTestServlet extends ScalatraServlet {
  post("/request-body") {
    val body = request.body
    val body2 = request.body
    response.setHeader("X-Idempotent", (body == body2).toString)
    body
  }
}

@RunWith(classOf[JUnitRunner])
class RequestBodyTest extends ScalatraFunSuite {
  addServlet(classOf[RequestBodyTestServlet], "/*")

  test("can read request body") {
    post("/request-body", "My cat's breath smells like cat food!") {
      body should equal("My cat's breath smells like cat food!")
    }
  }

  test("request body is idempotent") {
    post("/request-body", "Miss Hoover, I glued my head to my shoulder.") {
      header("X-Idempotent") should equal("true")
    }
  }
}
