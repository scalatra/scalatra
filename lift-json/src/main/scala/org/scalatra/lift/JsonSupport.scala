package org.scalatra.lift

import org.scalatra.ScalatraKernel
import net.liftweb.json.JsonAST.JValue

/**
 * Trait which renders Lift JValues as JSON.
 */
trait JsonSupport extends ScalatraKernel {

  override protected def contentTypeInfer = {
    case _ : JValue => "application/json; charset=utf-8"
  }

  override protected def renderPipeline = {
    case jv : JValue =>
      import net.liftweb._
      val bytes = json.compact(json.render(jv)).getBytes("UTF-8")
      response.getOutputStream.write(bytes)
  }
}