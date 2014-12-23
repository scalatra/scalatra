package org.scalatra

import org.scalatra.test.specs2.MutableScalatraSpec

class RouteMetadataSpec extends MutableScalatraSpec {
  addServlet(RouteMetadataSpec.servlet, "/*")

  "A route without metadata transformers" should {
    "not have any metadata" in {
      get("/zero/size") { body must_== "0" }
    }
  }

  "A route with a metadata transformer" should {
    "record the metadata" in {
      get("/one/foo") { body must_== "bar" }
    }
  }

  "A route with two metadata transformers" should {
    "apply left to right" in {
      get("/two/foo") { body must_== "baz" }
    }
  }
}

object RouteMetadataSpec {
  def meta(key: Symbol, value: String): RouteTransformer = { (route: Route) =>
    route.copy(metadata = route.metadata + (key -> value))
  }

  def servlet = new ScalatraServlet {
    val zero: Route = get("/zero/:key") {
      zero.metadata.size.toString
    }

    val one: Route = get("/one/:key", meta('foo, "bar")) {
      renderMeta(one, Symbol(params("key")))
    }

    val two: Route = get("/two/:key", meta('foo, "bar"), meta('foo, "baz")) {
      renderMeta(two, Symbol(params("key")))
    }

    def renderMeta(route: Route, key: Symbol) =
      route.metadata.getOrElse(key, "None")
  }
}
