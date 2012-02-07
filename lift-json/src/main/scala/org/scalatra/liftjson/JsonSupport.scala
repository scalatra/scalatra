package org.scalatra
package liftjson

import org.scalatra.{ContentTypeInferrer, ScalatraKernel}
import net.liftweb.json.JsonAST.JValue

/**
 * Trait which renders Lift JValues as JSON.
 */
trait JsonSupport extends ScalatraKernel {

  /**
   * If a request is made with a parameter in jsonpCallbackParameterNames it will
   * be assumed that it is a JSONP request and the json will be returned as the
   * argument to a function with the name specified in the corresponding parameter.
   *
   * By default no parameterNames will be checked
   */
  def jsonpCallbackParameterNames:  Iterable[String] = None

  override protected def contentTypeInferrer = ({
    case _ : JValue => "application/json; charset=utf-8"
  }: ContentTypeInferrer) orElse super.contentTypeInferrer

  override protected def renderPipeline = ({
    case jv : JValue =>
      import net.liftweb._
      val jsonString = json.compact(json.render(jv))

      val jsonWithCallback = for {
        paramName <- jsonpCallbackParameterNames
        callback <- params.get(paramName)
      } yield "%s(%s);" format (callback, jsonString)

      (jsonWithCallback.headOption.getOrElse(jsonString)).getBytes("UTF-8")
  }: RenderPipeline) orElse super.renderPipeline
}
