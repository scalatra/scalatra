package org.scalatra

trait UrlGenerator {

  def generate(route: Route, params: Pair[String, String]*): String =
    generate(route, params.toMap)

  def generate(route: Route, splat: String, moreSplats: String*): String =
    generate(route, Map[String, String](), splat +: moreSplats)

  def generate(
    route: Route,
    params: Map[String, String] = Map(),
    splats: Iterable[String] = Seq()
  ): String =
    route.reversibleMatcher match {
      case Some(matcher: ReversibleRouteMatcher) => matcher.reverse(params, splats.toList)
      case None =>
        throw new Exception("Route \"%s\" is not reversible" format (route))
    }
}
