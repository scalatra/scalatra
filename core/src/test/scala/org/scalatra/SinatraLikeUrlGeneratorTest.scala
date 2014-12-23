package org.scalatra

import org.scalatest.{ FunSuite, Matchers }

class SinatraLikeUrlGeneratorTest extends FunSuite with Matchers {

  def url(path: String, params: Tuple2[String, String]*): String =
    url(path, params.toMap)

  def url(path: String, splat: String, moreSplats: String*): String =
    url(path, Map[String, String](), splat +: moreSplats)

  def url(path: String, params: Map[String, String] = Map(), splats: Iterable[String] = Seq()): String =
    new SinatraRouteMatcher(path).reverse(params, splats.toList)

  test("Simple string route reverses to the same url") {
    url("/foo") should equal("/foo")
  }

  test("Route with one named parameter replaces the parameter") {
    url("/foo/:bar", "bar" -> "bryan") should equal("/foo/bryan")
  }

  test("Route with two named parameters replaces both parameters") {
    url("/foo/:bar/and/:rebar", "bar" -> "moonless", "rebar" -> "midnight") should equal("/foo/moonless/and/midnight")
  }

  test("Named of path containing a dot get replaced anyway") {
    url("/dot-outside-named-param/:file.:ext", "file" -> "TroutLauncher", "ext" -> "scala") should equal("/dot-outside-named-param/TroutLauncher.scala")
  }

  test("Missing parameter produces an exception") {
    the[Exception] thrownBy {
      url("/:bar/:baz", "bar" -> "moonless")
    }
  }

  test("Optional parameters can be provided") {
    url("/optional/:foo/:bar", "foo" -> "a", "bar" -> "b") should equal("/optional/a/b")
  }

  test("Optional parameters can be partially missing") {
    url("/optional/?:foo?/?:bar?", "foo" -> "a") should equal("/optional/a")
    url("/optional/?:foo?/?:bar?", "bar" -> "b") should equal("/optional/b")
  }

  test("Optional parameters can be all missing") {
    url("/optional/?:foo?/?:bar?") should equal("/optional")
  }

  test("Optional parameter following a dot can drop the dot") {
    url("/optional-ext.?:ext?") should equal("/optional-ext")
    url("/optional-ext.?:ext?", "ext" -> "json") should equal("/optional-ext.json")
  }

  test("Unexpected parameters are added as query string") {
    url("/foo/:bar", "bar" -> "pepper", "unexpected" -> "surprise") should equal("/foo/pepper?unexpected=surprise")
  }

  test("One splat parameter gets replaced") {
    url("/single-splat/*", "malt") should equal("/single-splat/malt")
  }

  test("Many splat parameters get replaced") {
    url("/mixing-multiple-splats/*/foo/*/*", "made", "in", "japan") should equal("/mixing-multiple-splats/made/foo/in/japan")
  }

  test("Mix named and splat") {
    url("/mix-named-and-splat-params/:foo/*", Map("foo" -> "deep"), Seq("purple")) should equal("/mix-named-and-splat-params/deep/purple")
  }

  test("Unexpected splat parameters trigger an exception") {
    the[Exception] thrownBy {
      url("/foo/*", "black", "coffee")
    }
  }
}
