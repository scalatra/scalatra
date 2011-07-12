package org.scalatra.liftjson

import org.scalatra.ScalatraKernel
import net.liftweb.json.JsonAST.JValue

/**
 * Trait which renders Lift JValues as JSON.
 */
trait JsonSupport extends ScalatraKernel {

  override protected def contentTypeInfer = {
    case _ : JValue => "application/json; charset=utf-8"
  }

  // TODO I've got better fights to pick right now than the type inferencer...
  // is there a way to make myPipeline anonymous without running into:
  // 
  // "The argument types of an anonymous function must be fully known. (SLS 8.5)"
  override protected def renderPipeline = {
    val myPipeline: PartialFunction[Any, Any] = {
      case jv : JValue =>
        import net.liftweb._
        val bytes = json.compact(json.render(jv)).getBytes("UTF-8")
        response.getOutputStream.write(bytes)
    }
    myPipeline orElse super.renderPipeline
  }
}
