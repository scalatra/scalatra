package org.scalatra.atmosphere

import org.json4s._

class NativeSimpleWireFormat(implicit formats: Formats) extends SimpleJsonWireFormat with native.JsonMethods {
  protected def renderJson(json: JsonAST.JValue): String = compact(render(json))
}