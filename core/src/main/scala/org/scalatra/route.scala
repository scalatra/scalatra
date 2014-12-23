package org.scalatra

import javax.servlet.http.HttpServletRequest

import org.scalatra.util.MultiMap

/**
 * A route is a set of matchers and an action.  A route is considered to match
 * if all of its route matchers return Some.  If a route matches, its action
 * may be invoked.  The route parameters extracted by the matchers are made
 * available to the action.
 */
case class Route(
    routeMatchers: Seq[RouteMatcher] = Seq.empty,
    action: Action,
    contextPath: HttpServletRequest => String = _ => "",
    metadata: Map[Symbol, Any] = Map.empty) {
  /**
   * Optionally returns this route's action and the multi-map of route
   * parameters extracted from the matchers.  Each matcher's returned params
   * are merged into those of the previous.  If any matcher returns None,
   * None is returned.  If there are no route matchers, some empty map is
   * returned.
   */
  def apply(requestPath: String): Option[MatchedRoute] = {
    routeMatchers.foldLeft(Option(MultiMap())) {
      (acc: Option[MultiParams], routeMatcher: RouteMatcher) =>
        for {
          routeParams <- acc
          matcherParams <- routeMatcher(requestPath)
        } yield routeParams ++ matcherParams
    } map { routeParams => MatchedRoute(action, routeParams) }
  }

  /**
   * The reversible matcher of a route is the first reversible matcher, if
   * any.  This matcher may be used to generate URIs.
   */
  lazy val reversibleMatcher: Option[RouteMatcher] =
    routeMatchers find (_.isInstanceOf[ReversibleRouteMatcher])

  /**
   * Determines whether this is a reversible route.
   */
  lazy val isReversible: Boolean = !reversibleMatcher.isEmpty

  override def toString: String = routeMatchers mkString " "
}

object Route {
  def apply(transformers: Seq[RouteTransformer], action: Action): Route =
    apply(transformers, action, (_: HttpServletRequest) => "")

  def apply(transformers: Seq[RouteTransformer], action: Action, contextPath: HttpServletRequest => String): Route = {
    val route = Route(action = action, contextPath = contextPath)
    transformers.foldLeft(route) { (route, transformer) => transformer(route) }
  }

  def appendMatcher(matcher: RouteMatcher): RouteTransformer = { (route: Route) =>
    route.copy(routeMatchers = route.routeMatchers :+ matcher)
  }
}

/**
 * An action and the multi-map of route parameters to invoke it with.
 */
case class MatchedRoute(action: Action, multiParams: MultiParams)
