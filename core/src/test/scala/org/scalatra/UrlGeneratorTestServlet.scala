package org.scalatra

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
