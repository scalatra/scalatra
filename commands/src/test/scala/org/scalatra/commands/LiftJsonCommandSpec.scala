package org.scalatra
package commands

import org.json4s._
import org.scalatra.json._

class NativeJsonTestForm extends JsonCommand {
  protected implicit val jsonFormats = DefaultFormats
  val name: Field[String] = asString("name").minLength(5)
  val quantity: Field[Int] = asInt("quantity").greaterThan(3)
}

class NativeJsonCommandSpecServlet extends ScalatraServlet with NativeJsonSupport with NativeJsonParsing {

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