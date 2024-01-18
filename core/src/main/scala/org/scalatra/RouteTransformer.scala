package org.scalatra

import scala.language.implicitConversions

object RouteTransformer {

  implicit def fn2transformer(fn: Route => Route): RouteTransformer =
    (route: Route) => fn(route)
}

trait RouteTransformer {
  def apply(route: Route): Route
}
