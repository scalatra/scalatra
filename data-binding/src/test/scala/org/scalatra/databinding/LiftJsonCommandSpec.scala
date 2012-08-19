package org.scalatra
package databinding

import liftjson.LiftJsonSupport
import net.liftweb.json.DefaultFormats


class LiftJsonTestForm extends LiftJsonCommand with JsonTestFields {
  protected implicit val jsonFormats = DefaultFormats
}

class LiftJsonCommandSpecServlet extends ScalatraServlet with LiftJsonSupport with CommandSupport with LiftJsonParsing {


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


class LiftJsonCommandSpec extends JsonCommandSpec("LiftJson", new LiftJsonCommandSpecServlet)