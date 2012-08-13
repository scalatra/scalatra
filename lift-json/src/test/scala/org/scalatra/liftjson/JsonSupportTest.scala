package org.scalatra
package liftjson

import org.scalatra.ScalatraServlet
import net.liftweb.json.JsonAST.JValue

class JsonSupportTest extends json.JsonSupportTestBase {
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


}


class JsonSupportTestServlet extends ScalatraServlet with LiftJsonSupport {
  get("/json") {
    import net.liftweb.json.JsonDSL._
    ("k1" -> "v1") ~
      ("k2" -> "v2")
  }


  get("/nulls") {
    null.asInstanceOf[JValue]
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
