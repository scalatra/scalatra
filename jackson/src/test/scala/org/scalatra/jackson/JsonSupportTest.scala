package org.scalatra.jackson

import org.scalatra.{json, ScalatraServlet}

class JsonSupportTest extends json.JsonSupportTestBase {
  override protected def expectedXml = """<?xml version='1.0' encoding='UTF-8'?>
                                  |<resp><k1 xmlns="">v1</k1><k2 xmlns="">v2</k2></resp>""".stripMargin
  addServlet(classOf[JsonSupportTestServlet], "/*")
  addServlet(classOf[JsonPTestServlet], "/p/*")
  addServlet(new ScalatraServlet with JacksonOutput {
    override protected lazy val jsonVulnerabilityGuard: Boolean = true
    override val jsonpCallbackParameterNames: Iterable[String] = Some("callback")
    get("/json") {
      jsonMapper.createObjectNode.put("k1", "v1").put("k2", "v2")
    }

    get("/jsonp") {
      jsonMapper.createObjectNode.put("k1", "v1").put("k2", "v2")
    }

  }, "/g/*")


}


class JsonSupportTestServlet extends ScalatraServlet with JacksonOutput {
  get("/json") {
    jsonMapper.createObjectNode.put("k1", "v1").put("k2", "v2")
  }
}

class JsonPTestServlet extends ScalatraServlet with JacksonOutput {
  override def jsonpCallbackParameterNames = Some("callback")

  get("/jsonp") {
    jsonMapper.createObjectNode.put("k1", "v1").put("k2", "v2")
  }
}
