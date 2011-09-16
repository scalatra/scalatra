package org.scalatra

/**
 * Adds support for generating URIs from routes and their params.
 */
trait UrlGeneratorSupport {

  /**
   * Calculate a URL for a reversible route and some params.
   *
   * @param route a reversible route
   * @param params a list of named param/value pairs
   * @return a URI that matches the route for the given params
   * @throws Exception if the route is not reversible
   */
  def url(route: Route, params: Pair[String, String]*): String =
    url(route, params.toMap)

  /**
   * Calculate a URL for a reversible route and some splats.
   *
   * @param route a reversible route
   * @param splat the first splat parameter
   * @param moreSplats any splat parameters beyond the first
   * @return a URI that matches the route for the given splats
   * @throws Exception if the route is not reversible
   */
  def url(route: Route, splat: String, moreSplats: String*): String =
    url(route, Map[String, String](), splat +: moreSplats)

  /**
   * Calculate a URL for a reversible route, some params, and some splats.
   *
   * @param route a reversible route
   * @param params a map of param/value pairs
   * @param splats a series of splat parameters
   * @return a URI that matches the route for the given splats
   * @throws Exception if the route is not reversible
   */
  def url(
    route: Route,
    params: Map[String, String] = Map(),
    splats: Iterable[String] = Seq()
  ): String =
    route.reversibleMatcher match {
      case Some(matcher: ReversibleRouteMatcher) => route.contextPath() + matcher.reverse(params, splats.toList)
      case None =>
        throw new Exception("Route \"%s\" is not reversible" format (route))
    }
}

object UrlGenerator extends UrlGeneratorSupport
