package org.scalatra
package databinding

import org.scalatra.json._
import org.json4s._


class NativeJsonTestForm extends JsonCommand with JsonTestFields {
  protected implicit val jsonFormats = DefaultFormats
}

class NativeJsonCommandSpecServlet extends ScalatraServlet with NativeJsonSupport with CommandSupport with NativeJsonParsing {

  implicit val jsonFormats: Formats = DefaultFormats

  post("/valid") {
    val cmd = command[NativeJsonTestForm]
    cmd.name.value.get + ":" + cmd.quantity.value.get
  }

  post("/invalid") {
    val cmd = command[NativeJsonTestForm]
    if (cmd.isInvalid) "OK"
    else "FAIL"
  }

  error {
    case e =>
      e.printStackTrace()
      throw e
  }

}


class NativeJsonCommandSpec extends JsonCommandSpec("NativeJson", new NativeJsonCommandSpecServlet)