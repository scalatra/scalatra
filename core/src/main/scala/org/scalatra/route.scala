package org.scalatra

import ScalatraKernel.{Action, MultiParams}
import util.MultiMap

/**
 * A route is a set of matchers and an action.  A route is considered to match
 * if all of its route matchers return Some.  If a route matches, its action
 * may be invoked.  The route parameters extracted by the matchers are made
 * available to the action.
 */
case class Route(
  routeMatchers: Iterable[RouteMatcher],
  action: Action,
  contextPath: () => String = () => ""
)
{
  /**
   * Optionally returns this route's action and the multi-map of route
   * parameters extracted from the matchers.  Each matcher's returned params
   * are merged into those of the previous.  If any matcher returns None,
   * None is returned.  If there are no route matchers, some empty map is
   * returned.
   */
  def apply(): Option[MatchedRoute] = {
    routeMatchers.foldLeft(Option(MultiMap())) {
      (acc: Option[MultiParams], routeMatcher: RouteMatcher) => for {
        routeParams <- acc
        matcherParams <- routeMatcher()
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

/**
 * An action and the multi-map of route parameters to invoke it with.
 */
case class MatchedRoute(action: Action, multiParams: MultiParams)
