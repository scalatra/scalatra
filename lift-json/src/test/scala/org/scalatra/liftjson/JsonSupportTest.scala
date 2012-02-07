package org.scalatra.liftjson

import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatra.ScalatraServlet

class JsonSupportTest extends ScalatraFunSuite {
  addServlet(classOf[JsonSupportTestServlet], "/*")
  addServlet(classOf[JsonPTestServlet], "/p/*")

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


class JsonSupportTestServlet extends ScalatraServlet with JsonSupport {
  get("/json") {
    import net.liftweb.json.JsonDSL._
    ("k1" -> "v1") ~
      ("k2" -> "v2")
  }
}

class JsonPTestServlet extends ScalatraServlet with JsonSupport {
  override def jsonpCallbackParameterNames = Some("callback")

  get("/jsonp") {
    import net.liftweb.json.JsonDSL._
    ("k1" -> "v1") ~
    ("k2" -> "v2")
  }
}
