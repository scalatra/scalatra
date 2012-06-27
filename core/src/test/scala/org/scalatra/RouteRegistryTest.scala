package org.scalatra

import org.specs2.Specification

object RouteRegistryTestApp extends ScalatraApp {
  get("/foo") { }
  post("/foo/:bar") { }
  put("""^/foo.../bar$""".r) { }
  get("/nothing", false) { }
  get(false) { }

  def renderRouteRegistry: String = routes.toString

  protected var doNotFound: _root_.org.scalatra.Action = () => NotFound()
}

class RouteRegistryTest extends Specification { def is =

  "route registry string representation contains the entry points" ! {
    RouteRegistryTestApp.renderRouteRegistry must_== (List(
      "GET /foo",
      "GET /nothing [Boolean Guard]",
      "GET [Boolean Guard]",
      "POST /foo/:bar",
      "PUT ^/foo.../bar$"
    ) mkString ", ")
  }
}
