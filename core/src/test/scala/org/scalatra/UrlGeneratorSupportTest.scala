package org.scalatra

import test.scalatest.ScalatraFunSuite

trait UrlGeneratorContextTestServlet extends ScalatraServlet {

  val foo: Route = get("/foo") { UrlGenerator.url(foo) }
}

class UrlGeneratorSupportTest extends ScalatraFunSuite {

  addServlet(new UrlGeneratorContextTestServlet {}, "/*")

  addServlet(new UrlGeneratorContextTestServlet {}, "/context/*")

  test("Url of a servlet mounted on /*") {
    get("/foo") {
      body should equal ("/foo")
    }
  }

  test("Url of a servlet mounted on /context/*") {
    get("/context/foo") {
      body should equal ("/context/foo")
    }
  }
}
