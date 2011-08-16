package org.scalatra

import test.scalatest.ScalatraFunSuite

class UrlGeneratorTestServlet extends ScalatraServlet
{
  val simpleString = get("/foo") { }

  val singleNamed = get("/foo/:bar") { }

  val multipleNameds = get("/foo/:bar/and/:rebar") { }

  val booleanTest = get(params.getOrElse("booleanTest", "false") == "true") { }

  val optional = get("/optional/?:foo?/?:bar?") { }

  val singleSplat = get("/single-splat/*") { }

  val multipleSplats = get("/mixing-multiple-splats/*/foo/*/*") { }

  val mixNamedAndSplat = get("/mix-named-and-splat-params/:foo/*") { }

  val dotInNamedParam = get("/dot-in-named-param/:foo/:bar") { }

  val dotOutsideNamedParam = get("/dot-outside-named-param/:file.:ext") { }

  val literalDotInPath = get("/literal.dot.in.path") { }

  val stringAndBoolean = get("/conditional", params.getOrElse("condition", "false") == "true") { }

  val regex1 = get("""^\/fo(.*)/ba(.*)""".r) { }

  val regex2 = get("""^/foo.../bar$""".r) { }

  val anyPost = post() { }

  val pathPattern = get(new PathPattern(".".r, Nil)) { }

  val customMatcher = get(new RouteMatcher { def apply() = None }) { }

  val stringAndCustomMatcher = get("/fail", new RouteMatcher { def apply() = None }) { }
}

class UrlGeneratorTest extends ScalatraFunSuite {
  addServlet(classOf[UrlGeneratorTestServlet], "/*")

  object TestUrlGenerator extends UrlGenerator

  object S extends UrlGeneratorTestServlet

  def url(route: Route, params: Map[String, String] = Map()): String =
    TestUrlGenerator.generate(route, params)

  test("Urls can be generated from a string") {
    S.simpleString.canGenerate should equal (true)
  }

  test("Urls can be generated from a string with one named") {
    S.singleNamed.canGenerate should equal (true)
  }

  test("Urls can be generated from a string with two named") {
    S.multipleNameds.canGenerate should equal (true)
  }

  test("Urls can be generated from a string with one splat") {
    S.singleSplat.canGenerate should equal (true)
  }

  test("Urls can be generated from a string with two splats") {
    S.multipleSplats.canGenerate should equal (true)
  }

  test("Urls can be generated from a string and a boolean block") {
    S.stringAndBoolean.canGenerate should equal (true)
  }

  test("Urls can be generated from a string and a custom matcher") {
    S.stringAndCustomMatcher.canGenerate should equal (true)
  }

  test("Urls can not be generated from no matchers") {
    S.anyPost.canGenerate should equal (false)
  }

  test("Urls can not be generated from only a boolean block") {
    S.booleanTest.canGenerate should equal (false)
  }

  test("Urls can not be generated from a regex") {
    S.regex1.canGenerate should equal (false)
    S.regex2.canGenerate should equal (false)
  }

  test("Urls can not be generated from a path pattern") {
    S.pathPattern.canGenerate should equal (false)
  }

  test("Urls can not be generated from a a custom matcher") {
    S.customMatcher.canGenerate should equal (false)
  }
}
