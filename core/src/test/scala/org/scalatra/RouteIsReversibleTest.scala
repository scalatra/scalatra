package org.scalatra

import org.scalatra.test.scalatest.ScalatraFunSuite

class RouteIsReversibleTest extends ScalatraFunSuite {
  addServlet(classOf[UrlGeneratorTestServlet], "/*")

  object TestServlet extends UrlGeneratorTestServlet
  import TestServlet._

  test("Urls can be generated from a string") {
    simpleString.isReversible should equal(true)
  }

  test("Urls can be generated from a string with one named") {
    singleNamed.isReversible should equal(true)
  }

  test("Urls can be generated from a string with two named") {
    multipleNameds.isReversible should equal(true)
  }

  test("Urls can be generated from a string with one splat") {
    singleSplat.isReversible should equal(true)
  }

  test("Urls can be generated from a string with two splats") {
    multipleSplats.isReversible should equal(true)
  }

  test("Urls can be generated from a string with mixed named and splats") {
    mixNamedAndSplat.isReversible should equal(true)
  }

  test("Urls can be generated from a string and a boolean block") {
    stringAndBoolean.isReversible should equal(true)
  }

  test("Urls can be generated from a string and a custom matcher") {
    stringAndCustomMatcher.isReversible should equal(true)
  }

  test("Urls can not be generated from no matchers") {
    anyPost.isReversible should equal(false)
  }

  test("Urls can not be generated from only a boolean block") {
    booleanTest.isReversible should equal(false)
  }

  test("Urls can not be generated from a regex") {
    regex1.isReversible should equal(false)
    regex2.isReversible should equal(false)
  }

  test("Urls can not be generated from a path pattern") {
    pathPattern.isReversible should equal(false)
  }

  test("Urls can not be generated from a a custom matcher") {
    customMatcher.isReversible should equal(false)
  }
}
