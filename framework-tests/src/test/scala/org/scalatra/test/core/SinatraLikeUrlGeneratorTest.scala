//package org.scalatra
//
//import org.scalatest.FunSuite
//import org.scalatest.matchers.MustMatchers
//
//class SinatraLikeUrlGeneratorTest extends FunSuite with MustMatchers {
//
//  def url(path: String, params: Pair[String, String]*): String =
//    url(path, params.toMap)
//
//  def url(path: String, splat: String, moreSplats: String*): String =
//    url(path, Map[String, String](), splat +: moreSplats)
//
//  def url(path: String, params: Map[String, String] = Map(), splats: Iterable[String] = Seq()): String =
//    new SinatraRouteMatcher(path, "/").reverse(params, splats.toList)
//
//  test("Simple string route reverses to the same url") {
//    url("/foo") must equal ("/foo")
//  }
//
//  test("Route with one named parameter replaces the parameter") {
//    url("/foo/:bar", "bar" -> "bryan") must equal ("/foo/bryan")
//  }
//
//  test("Route with two named parameters replaces both parameters") {
//    url("/foo/:bar/and/:rebar", "bar" -> "moonless", "rebar" -> "midnight") must equal ("/foo/moonless/and/midnight")
//  }
//
//  test("Named of path containing a dot get replaced anyway") {
//    url("/dot-outside-named-param/:file.:ext", "file" -> "TroutLauncher", "ext" -> "scala") must equal ("/dot-outside-named-param/TroutLauncher.scala")
//  }
//
//  test("Missing parameter produces an exception") {
//    evaluating {
//      url("/:bar/:baz", "bar" -> "moonless")
//    } must produce [Exception]
//  }
//
//  test("Optional parameters can be provided") {
//    url("/optional/:foo/:bar", "foo" -> "a", "bar" -> "b") must equal ("/optional/a/b")
//  }
//
//  test("Optional parameters can be partially missing") {
//    url("/optional/?:foo?/?:bar?", "foo" -> "a") must equal ("/optional/a")
//    url("/optional/?:foo?/?:bar?", "bar" -> "b") must equal ("/optional/b")
//  }
//
//  test("Optional parameters can be all missing") {
//    url("/optional/?:foo?/?:bar?") must equal ("/optional")
//  }
//
//  test("Optional parameter following a dot can drop the dot") {
//    url("/optional-ext.?:ext?") must equal ("/optional-ext")
//    url("/optional-ext.?:ext?", "ext" -> "json") must equal ("/optional-ext.json")
//  }
//
//  test("Unexpected parameters are added as query string") {
//    url("/foo/:bar", "bar" -> "pepper", "unexpected" -> "surprise") must equal ("/foo/pepper?unexpected=surprise")
//  }
//
//  test("One splat parameter gets replaced") {
//    url("/single-splat/*", "malt") must equal ("/single-splat/malt")
//  }
//
//  test("Many splat parameters get replaced") {
//    url("/mixing-multiple-splats/*/foo/*/*", "made", "in", "japan") must equal ("/mixing-multiple-splats/made/foo/in/japan")
//  }
//
//  test("Mix named and splat") {
//    url("/mix-named-and-splat-params/:foo/*", Map("foo" -> "deep"), Seq("purple")) must equal ("/mix-named-and-splat-params/deep/purple")
//  }
//
//  test("Unexpected splat parameters trigger an exception") {
//    evaluating {
//      url("/foo/*", "black", "coffee")
//    } must produce [Exception]
//  }
//}
