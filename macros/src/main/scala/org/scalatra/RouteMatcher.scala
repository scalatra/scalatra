package org.scalatra

/**
 * A route matcher is evaluated in the context it was created and returns a
 * a (possibly empty) multi-map of parameters if the route is deemed to match.
 */
trait RouteMatcher extends RouteTransformer {
  def apply(requestPath: String): Option[MultiParams]

  def apply(route: Route): Route = Route.appendMatcher(this)(route)
}

/**
 * A route matcher from which a URI can be generated from route parameters.
 */
trait ReversibleRouteMatcher {
  /**
   * Generates a URI from a route matcher.
   *
   * @param params a map of named params extractable by the route
   * @param splats a list of splats extractable by the route
   * @return a String that would match the route with the given params and splats
   */
  def reverse(params: Map[String, String], splats: List[String]): String
}
