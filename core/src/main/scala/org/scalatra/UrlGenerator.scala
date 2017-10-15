package org.scalatra

import javax.servlet.http.HttpServletRequest

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
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(route: Route, params: Tuple2[String, String]*)(implicit req: HttpServletRequest): String =
    url(route, params.toMap, Seq.empty)

  /**
   * Calculate a URL for a reversible route and some splats.
   *
   * @param route a reversible route
   * @param splat the first splat parameter
   * @param moreSplats any splat parameters beyond the first
   * @return a URI that matches the route for the given splats
   * @throws Exception if the route is not reversible
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(route: Route, splat: String, moreSplats: String*)(implicit req: HttpServletRequest): String =
    url(route, Map[String, String](), splat +: moreSplats)

  /**
   * Calculate a URL for a reversible route, some params, and some splats.
   *
   * @param route a reversible route
   * @param params a map of param/value pairs
   * @param splats a series of splat parameters
   * @return a URI that matches the route for the given splats
   * @throws Exception if the route is not reversible
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(
    route: Route,
    params: Map[String, String],
    splats: Iterable[String])(implicit req: HttpServletRequest): String =
    route.reversibleMatcher match {
      case Some(matcher: ReversibleRouteMatcher) =>
        route.contextPath(req) + matcher.reverse(params, splats.toList)
      case _ =>
        throw new Exception("Route \"%s\" is not reversible" format (route))
    }
}

object UrlGenerator extends UrlGeneratorSupport
