package org.scalatra

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

class RequestBodyTestApp extends ScalatraApp {
  post("/request-body") {
    val body = request.body
    val body2 = request.body
    response.headers += "X-Idempotent" -> (body == body2).toString
    body
  }
}

@RunWith(classOf[JUnitRunner])
abstract class RequestBodyTest extends ScalatraFunSuite {
  mount(new RequestBodyTestApp)

  test("can read request body") {
    post("/request-body", body = "My cat's breath smells like cat food!", headers = Map("Content-Type" -> "text/plain")) {
      body should equal ("My cat's breath smells like cat food!")
    }
  }

  test("request body is idempotent") {
    post("/request-body", "Miss Hoover, I glued my head to my shoulder.") {
      headers("X-Idempotent") should equal ("true")
    }
  }
}

class NettyRequestBodyTest extends RequestBodyTest with NettyBackend
class JettyRequestBodyTest extends RequestBodyTest with JettyBackend
