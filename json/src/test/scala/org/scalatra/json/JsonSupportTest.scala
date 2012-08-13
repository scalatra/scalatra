package org.scalatra
package json

import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatra.ScalatraServlet

abstract class JsonSupportTestBase extends ScalatraFunSuite {
  protected def expectedXml = """<?xml version='1.0' encoding='UTF-8'?>
                                |<resp><k1>v1</k1><k2>v2</k2></resp>""".stripMargin

  test("JSON support test") {
    get("/json") {
      response.mediaType should equal (Some("application/json"))
      response.body should equal ("""{"k1":"v1","k2":"v2"}""")
    }
  }

  test("Don't panic on null") {
    get("/nulls", headers = Map("Accept" -> "application/json")) {
      response.mediaType should equal (Some("application/json"))
      response.status should equal (200)
    }
  }


  test("Don't panic on null XML") {
    get("/nulls", headers = Map("Accept" -> "application/xml")) {
      response.mediaType should equal (Some("application/xml"))
      response.status should equal (200)
    }
  }

  test("XML output of a JValue") {
    get("/json", headers = Map("Accept" -> "application/xml")) {
      response.mediaType should equal (Some("application/xml"))
      response.body should equal (expectedXml)
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
