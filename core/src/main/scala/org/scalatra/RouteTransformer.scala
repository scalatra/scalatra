package org.scalatra

object RouteTransformer {

  implicit def fn2transformer(fn: Route => Route): RouteTransformer = new RouteTransformer {
    override def apply(route: Route): Route = fn(route)
  }
}

trait RouteTransformer {
  def apply(route: Route): Route
}
