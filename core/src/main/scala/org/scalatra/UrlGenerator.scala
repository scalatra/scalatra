package org.scalatra

trait UrlGeneratorSupport {

  def url(route: Route, params: Pair[String, String]*): String =
    url(route, params.toMap)

  def url(route: Route, splat: String, moreSplats: String*): String =
    url(route, Map[String, String](), splat +: moreSplats)

  def url(
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

object UrlGenerator extends UrlGeneratorSupport
