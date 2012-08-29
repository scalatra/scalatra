package org.scalatra
package databinding

import org.scalatra.json._
import org.json4s._


class LiftJsonTestForm extends JsonCommand with JsonTestFields {
  protected implicit val jsonFormats = DefaultFormats
}

class NativeJsonCommandSpecServlet extends ScalatraServlet with NativeJsonSupport with CommandSupport with NativeJsonParsing {

  implicit val jsonFormats: Formats = DefaultFormats

  post("/valid") {
    val cmd = command[LiftJsonTestForm]
    cmd.name.value.toOption.get + ":" + cmd.quantity.value.toOption.get
  }

  post("/invalid") {
    val cmd = command[LiftJsonTestForm]
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