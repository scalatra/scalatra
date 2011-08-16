package org.scalatra

import test.scalatest.ScalatraFunSuite

class UrlGeneratorTest extends ScalatraFunSuite {

  addServlet(classOf[UrlGeneratorTestServlet], "/*")

  object TestUrlGenerator extends UrlGenerator
  import TestUrlGenerator._

  object TestServlet extends UrlGeneratorTestServlet
  import TestServlet._

  test("Simple string routes reverse to the same url") {
    generate(simpleString) should equal ("/foo")
  }
}
