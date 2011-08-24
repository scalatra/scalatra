package org.scalatra
package liftjson

import org.scalatra.{ContentTypeInferrer, ScalatraKernel}
import net.liftweb.json.JsonAST.JValue

/**
 * Trait which renders Lift JValues as JSON.
 */
trait JsonSupport extends ScalatraKernel {

  override protected def contentTypeInferrer = ({
    case _ : JValue => "application/json; charset=utf-8"
  }: ContentTypeInferrer) orElse super.contentTypeInferrer

  override protected def renderPipeline = ({
    case jv : JValue =>
      import net.liftweb._
      json.compact(json.render(jv)).getBytes("UTF-8")
  }: RenderPipeline) orElse super.renderPipeline


}
