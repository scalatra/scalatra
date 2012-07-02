package org.scalatra
package liftjson

import org.scalatra.test.scalatest.ScalatraFunSuite
import test.NettyBackend

abstract class JsonSupportTest extends ScalatraFunSuite {
  mount(new JsonSupportTestApp)
  mount("/p", new JsonPTestApp)

  test("JSON support test") {
    get("/json") {
      response.mediaType should equal (Some("application/json"))
      response.body should equal ("""{"k1":"v1","k2":"v2"}""")
    }
  }

  test("JSONP callback test with no callback name specified") {
    get("/json", "callback" -> "function") {
      response.mediaType should equal (Some("application/json"))
      response.body should equal ("""{"k1":"v1","k2":"v2"}""")
    }
  }

  test("JSONP callback test with callback name specified") {
    get("/p/jsonp", "callback" -> "function") {
      response.mediaType should equal (Some("application/json"))
      response.body should equal ("""function({"k1":"v1","k2":"v2"});""")
    }
  }
}


class JsonSupportTestApp extends ScalatraApp with LiftJsonSupport {
  get("/json") {
    import net.liftweb.json.JsonDSL._
    ("k1" -> "v1") ~
      ("k2" -> "v2")
  }
}

class JsonPTestApp extends ScalatraApp with LiftJsonSupport {
  override def jsonpCallbackParameterNames = Some("callback")

  get("/jsonp") {
    import net.liftweb.json.JsonDSL._
    ("k1" -> "v1") ~
    ("k2" -> "v2")
  }
}

class NettyJsonSupportTest extends JsonSupportTest with NettyBackend