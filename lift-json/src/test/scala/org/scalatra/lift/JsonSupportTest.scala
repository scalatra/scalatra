package org.scalatra.lift

import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.matchers.ShouldMatchers
import org.scalatra.ScalatraServlet

class JsonSupportTest extends ScalatraFunSuite with ShouldMatchers {
  val servletHolder = addServlet(classOf[JsonSupportTestServlet], "/*")
  servletHolder.setInitOrder(1) // force load on startup

  test("JSON support test") {
    get("/json") {
      response.mediaType should equal (Some("application/json"))
      response.body should equal ("""{"k1":"v1","k2":"v2"}""")
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
