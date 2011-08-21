package org.scalatra

import ScalatraKernel.{Action, MultiParams}
import util.MultiMap

case class Route(
  routeMatchers: Iterable[RouteMatcher],
  action: Action,
  contextPath: () => String = () => ""
)
{
  def apply(): Option[MatchedRoute] = {
    routeMatchers.foldLeft(Option(MultiMap())) {
      (acc: Option[MultiParams], routeMatcher: RouteMatcher) => for {
        routeParams <- acc
        matcherParams <- routeMatcher()
      } yield routeParams ++ matcherParams
    } map { routeParams => MatchedRoute(action, routeParams) }
  }

  lazy val reversibleMatcher: Option[RouteMatcher] =
    routeMatchers find (_.isInstanceOf[ReversibleRouteMatcher])

  lazy val isReversible: Boolean = !reversibleMatcher.isEmpty

  override def toString: String = routeMatchers mkString " "
}

case class MatchedRoute(action: Action, multiParams: MultiParams)
