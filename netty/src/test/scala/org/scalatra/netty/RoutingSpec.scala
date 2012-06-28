package org.scalatra
package netty

import test.specs2.ScalatraSpec

class TestSupApp extends ScalatraApp {
  get("/") {
    "TestSub index"
  }

  get("/other") {
    "TestSub other"
  }
}

class TestScalatraApp extends ScalatraApp  {

  get("/") {
    "OMG! It works!!!"
  }

  get("/hello") {
    "world"
  }

  mount("/sub", new TestSupApp)
}
class RoutingSpec extends ScalatraSpec with NettyBackend {

  mount(new TestScalatraApp)

  def is =
    "A scalatra app should" ^
      "respond to an index request" ! root ^
      "respond to a pathed request" ! get("/hello") { response.body must_== "world" } ^
      "respond to a sub app index request" ! get("/sub") { response.body must_== "TestSub index" } ^
      "respond to a sub app pathed request" ! get("/sub/other") { response.body must_== "TestSub other" } ^
    end

  def root = get("/") {
    response.body must_== "OMG! It works!!!"
  }
}