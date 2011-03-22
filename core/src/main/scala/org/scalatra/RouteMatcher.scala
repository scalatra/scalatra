package org.scalatra

import ScalatraKernel.MultiParams
import util.MultiMap

object RouteMatcher {

  implicit def map2multimap(map: Map[String, Seq[String]]) = new MultiMap(map)
  def matchRoute(routeMatchers: Iterable[RouteMatcher]) = {
    routeMatchers.foldLeft(Option(MultiMap())) { (acc: Option[MultiParams], rm: RouteMatcher) =>
      for (x <- acc; y <- rm.apply()) yield x ++ y           
    }
  }

  implicit def fun2RouteMatcher(f: () => Option[MultiParams]) = new RouteMatcher { def apply() = f() }
}

trait RouteMatcher extends (() => Option[MultiParams]) 
