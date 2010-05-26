package com.thinkminimo.step

import StepKernel.MultiParams

object RouteMatcher {
  def matchRoute(routeMatchers: Iterable[RouteMatcher]) = {
    routeMatchers.foldLeft(Option(Map[String, Seq[String]]())) { (acc: Option[MultiParams], rm: RouteMatcher) =>
      for (x <- acc; y <- rm.apply()) yield x ++ y           
    }
  }

  implicit def fun2RouteMatcher(f: () => Option[MultiParams]) = new RouteMatcher { def apply() = f() }
}

trait RouteMatcher extends (() => Option[MultiParams]) 