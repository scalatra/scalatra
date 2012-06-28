package org.scalatra

import test.NettyBackend
import test.scalatest.ScalatraFunSuite

class RailsLikeRouteTestApp extends ScalatraApp {
  implicit override def string2RouteMatcher(path: String) =
    RailsPathPatternParser(path)

  // This syntax wouldn't work in Sinatra
  get("/:file(.:ext)") {
    List("file", "ext") foreach { param =>
      response.headers += param -> params.getOrElse(param, "")
    }
  }
}

abstract class RailsLikeRouteTest extends ScalatraFunSuite {
  mount(new RailsLikeRouteTestApp)

  test("matches without extension") {
    get("/foo") {
      headers("file") should equal ("foo")
      headers("ext") should equal ("")
    }
  }

  test("matches with extension") {
    get("/foo.xml") {
      headers("file") should equal ("foo")
      headers("ext") should equal ("xml")
    }
  }
}

class NettyRailsLikeRouteTest extends RailsLikeRouteTest with NettyBackend

