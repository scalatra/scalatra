package org.scalatra

import test.scalatest.ScalatraFunSuite

class RouteIsReversibleTest extends ScalatraFunSuite {
  addServlet(classOf[UrlGeneratorTestServlet], "/*")

  object TestUrlGenerator extends UrlGenerator

  object S extends UrlGeneratorTestServlet

  def url(route: Route, params: Map[String, String] = Map()): String =
    TestUrlGenerator.generate(route, params)

  test("Urls can be generated from a string") {
    S.simpleString.isReversible should equal (true)
  }

  test("Urls can be generated from a string with one named") {
    S.singleNamed.isReversible should equal (true)
  }

  test("Urls can be generated from a string with two named") {
    S.multipleNameds.isReversible should equal (true)
  }

  test("Urls can be generated from a string with one splat") {
    S.singleSplat.isReversible should equal (true)
  }

  test("Urls can be generated from a string with two splats") {
    S.multipleSplats.isReversible should equal (true)
  }

  test("Urls can be generated from a string with mixed named and splats") {
    S.mixNamedAndSplat.isReversible should equal (true)
  }

  test("Urls can be generated from a string and a boolean block") {
    S.stringAndBoolean.isReversible should equal (true)
  }

  test("Urls can be generated from a string and a custom matcher") {
    S.stringAndCustomMatcher.isReversible should equal (true)
  }

  test("Urls can not be generated from no matchers") {
    S.anyPost.isReversible should equal (false)
  }

  test("Urls can not be generated from only a boolean block") {
    S.booleanTest.isReversible should equal (false)
  }

  test("Urls can not be generated from a regex") {
    S.regex1.isReversible should equal (false)
    S.regex2.isReversible should equal (false)
  }

  test("Urls can not be generated from a path pattern") {
    S.pathPattern.isReversible should equal (false)
  }

  test("Urls can not be generated from a a custom matcher") {
    S.customMatcher.isReversible should equal (false)
  }
}
