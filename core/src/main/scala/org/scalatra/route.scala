package org.scalatra

import ScalatraKernel.{Action, MultiParams}
import util.MultiMap

case class Route(routeMatchers: Iterable[RouteMatcher], action: Action) {
  def apply(): Option[MatchedRoute] = {
    routeMatchers.foldLeft(Option(MultiMap())) {
      (acc: Option[MultiParams], routeMatcher: RouteMatcher) => for {
        routeParams <- acc
        matcherParams <- routeMatcher()
      } yield routeParams ++ matcherParams
    } map { routeParams => MatchedRoute(action, routeParams) }
  }

  override def toString: String = routeMatchers mkString " "
}

case class MatchedRoute(action: Action, multiParams: MultiParams)
