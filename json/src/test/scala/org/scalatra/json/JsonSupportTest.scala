package org.scalatra
package json

import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatra.ScalatraServlet
import org.json4s._

class JsonSupportTest extends json.JsonSupportTestBase {
  addServlet(new JsonSupportTestServlet, "/*")
  addServlet(new JsonPTestServlet, "/p/*")
  addServlet(new ScalatraServlet with NativeJsonSupport {

    implicit protected def jsonFormats: Formats = DefaultFormats

    override protected lazy val jsonVulnerabilityGuard: Boolean = true
    override val jsonpCallbackParameterNames: Iterable[String] = Some("callback")
    get("/json") {
      import org.json4s.JsonDSL._
      ("k1" -> "v1") ~
        ("k2" -> "v2")
    }

    get("/jsonp") {
      import org.json4s.JsonDSL._
      ("k1" -> "v1") ~
        ("k2" -> "v2")
    }

  }, "/g/*")

}

class JsonSupportTestServlet extends ScalatraServlet with NativeJsonSupport {

  get("/json") {
    import org.json4s.JsonDSL._
    ("k1" -> "v1") ~
      ("k2" -> "v2")
  }

  get("/json-in-action-result") {
    import org.json4s.JsonDSL._
    BadRequest(("k1" -> "v1") ~ ("k2" -> "v2"))
  }

  get("/json-result") {
    import org.json4s.JsonDSL._
    JsonResult(("k1" -> "v1"))
  }

  get("/json-result-in-action-result") {
    import org.json4s.JsonDSL._
    BadRequest(JsonResult(("error" -> "message")))
  }

  get("/nulls") {
    JNull
  }

  implicit protected def jsonFormats: Formats = DefaultFormats
}

class JsonPTestServlet extends ScalatraServlet with NativeJsonSupport {

  implicit protected def jsonFormats: Formats = DefaultFormats

  override def jsonpCallbackParameterNames = Some("callback")

  get("/jsonp") {
    import org.json4s.JsonDSL._
    ("k1" -> "v1") ~
      ("k2" -> "v2")
  }
}

abstract class JsonSupportTestBase extends ScalatraFunSuite {
  protected def expectedXml = """<?xml version='1.0' encoding='UTF-8'?>
                                |<resp><k1>v1</k1><k2>v2</k2></resp>""".stripMargin

  test("JSON support test") {
    get("/json") {
      response.mediaType should equal(Some("application/json"))
      response.body should equal("""{"k1":"v1","k2":"v2"}""")
    }
  }

  test("JSON in ActionResult test") {
    get("/json-in-action-result") {
      response.status should equal(400)
      response.mediaType should equal(Some("application/json"))
      response.body should equal("""{"k1":"v1","k2":"v2"}""")
    }
  }

  test("JsonResult test") {
    get("/json-result") {
      response.mediaType should equal(Some("application/json"))
      response.body should equal("""{"k1":"v1"}""")
    }
  }

  test("JsonResult in ActionResult test") {
    get("/json-result-in-action-result") {
      response.mediaType should equal(Some("application/json"))
      response.body should equal("""{"error":"message"}""")
      response.status should equal(400)
    }
  }

  test("Don't panic on null") {
    get("/nulls", headers = Map("Accept" -> "application/json")) {
      response.mediaType should equal(Some("application/json"))
      response.status should equal(200)
    }
  }

  test("Don't panic on null XML") {
    get("/nulls", headers = Map("Accept" -> "application/xml")) {
      response.mediaType should equal(Some("application/xml"))
      response.status should equal(200)
    }
  }

  test("XML output of a JValue") {
    get("/json", headers = Map("Accept" -> "application/xml")) {
      response.mediaType should equal(Some("application/xml"))
      response.body should equal(expectedXml)
    }
  }

  test("JSONP callback test with no callback name specified") {
    get("/json", "callback" -> "function") {
      response.mediaType should equal(Some("application/json"))
      response.body should equal("""{"k1":"v1","k2":"v2"}""")
    }
  }

  test("JSONP callback test with callback name specified") {
    get("/p/jsonp", "callback" -> "function") {
      response.mediaType should equal(Some("text/javascript"))
      response.body should equal("""/**/function({"k1":"v1","k2":"v2"});""")
    }
  }

  test("JSON vulnerability guard if enabled") {
    get("/g/json") {
      response.mediaType should equal(Some("application/json"))
      response.body should equal(")]}',\n" + """{"k1":"v1","k2":"v2"}""")
    }
  }

  test("JSONP callback test with callback name specified and guard enabled") {
    get("/p/jsonp", "callback" -> "function") {
      response.mediaType should equal(Some("text/javascript"))
      response.body should equal("""/**/function({"k1":"v1","k2":"v2"});""")
    }
  }

}
