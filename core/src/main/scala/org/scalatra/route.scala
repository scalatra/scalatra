package org.scalatra

import ScalatraKernel.{Action, MultiParams}
import util.MultiMap

object RouteMatcher {
  implicit def map2multimap(map: Map[String, Seq[String]]) = new MultiMap(map)

  implicit def fun2RouteMatcher(f: () => Option[MultiParams]) = new RouteMatcher { def apply() = f() }
}

trait RouteMatcher extends (() => Option[MultiParams]) 

case class Route(routeMatchers: Iterable[RouteMatcher], action: Action) {
  import RouteMatcher.map2multimap

  def apply(): Option[MatchedRoute] = 
    routeMatchers.foldLeft(Option(MultiMap())) { 
      (acc: Option[MultiParams], routeMatcher: RouteMatcher) => for { 
        routeParams <- acc
        matcherParams <- routeMatcher() 
      } yield routeParams ++ matcherParams
    } map { routeParams => MatchedRoute(action, routeParams) }
}

case class MatchedRoute(action: Action, multiParams: MultiParams)
