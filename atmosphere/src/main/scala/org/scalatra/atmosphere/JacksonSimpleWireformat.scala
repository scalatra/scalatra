package org.scalatra.atmosphere

import org.json4s._

class JacksonSimpleWireformat extends SimpleJsonWireFormat with jackson.JsonMethods {
  protected def renderJson(json: JsonAST.JValue): String = compact(render(json))
}