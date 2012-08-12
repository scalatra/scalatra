package org.scalatra.liftjson

import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatra.ScalatraServlet

class JsonSupportTest extends ScalatraFunSuite {
  addServlet(classOf[JsonSupportTestServlet], "/*")
  addServlet(classOf[JsonPTestServlet], "/p/*")
  addServlet(new ScalatraServlet with LiftJsonSupport {
    override protected lazy val jsonVulnerabilityGuard: Boolean = true
    override val jsonpCallbackParameterNames: Iterable[String] = Some("callback")
    get("/json") {
      import net.liftweb.json.JsonDSL._
      ("k1" -> "v1") ~
        ("k2" -> "v2")
    }

    get("/jsonp") {
      import net.liftweb.json.JsonDSL._
      ("k1" -> "v1") ~
        ("k2" -> "v2")
    }

  }, "/g/*")

  test("JSON support test") {
    get("/json") {
      response.mediaType should equal (Some("application/json"))
      response.body should equal ("""{"k1":"v1","k2":"v2"}""")
    }
  }

  test("XML output of a JValue") {
    get("/json", headers = Map("Accept" -> "application/xml")) {
      response.mediaType should equal (Some("application/xml"))
      response.body should equal (
        """<?xml version='1.0' encoding='UTF-8'?>
          |<resp><k1>v1</k1><k2>v2</k2></resp>""".stripMargin)
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
      response.mediaType should equal (Some("text/javascript"))
      response.body should equal ("""function({"k1":"v1","k2":"v2"});""")
    }
  }

  test("JSON vulnerability guard if enabled") {
    get("/g/json") {
      response.mediaType should equal (Some("application/json"))
      response.body should equal (")]}',\n"+"""{"k1":"v1","k2":"v2"}""")
    }
  }

  test("JSONP callback test with callback name specified and guard enabled") {
    get("/p/jsonp", "callback" -> "function") {
      response.mediaType should equal (Some("text/javascript"))
      response.body should equal ("""function({"k1":"v1","k2":"v2"});""")
    }
  }

}


class JsonSupportTestServlet extends ScalatraServlet with LiftJsonSupport {
  get("/json") {
    import net.liftweb.json.JsonDSL._
    ("k1" -> "v1") ~
      ("k2" -> "v2")
  }
}

class JsonPTestServlet extends ScalatraServlet with LiftJsonSupport {
  override def jsonpCallbackParameterNames = Some("callback")

  get("/jsonp") {
    import net.liftweb.json.JsonDSL._
    ("k1" -> "v1") ~
    ("k2" -> "v2")
  }
}
