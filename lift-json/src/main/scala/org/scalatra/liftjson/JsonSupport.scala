package org.scalatra
package liftjson

import net.liftweb.json._
import scala.io.Codec.UTF8
import net.liftweb.json.Xml.toXml
import xml.Utility.trimProper

@deprecated("Use LiftJsonSupport instead", "2.1.0")
trait JsonSupport extends LiftJsonOutput

private[liftjson] trait LiftJsonOutput extends ApiFormats {

  /**
   * If a request is made with a parameter in jsonpCallbackParameterNames it will
   * be assumed that it is a JSONP request and the json will be returned as the
   * argument to a function with the name specified in the corresponding parameter.
   *
   * By default no parameterNames will be checked
   */
  def jsonpCallbackParameterNames:  Iterable[String] = Nil

  /**
   * Whether or not to apply the jsonVulnerabilityGuard when rendering json.
   * @see http://haacked.com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx
   */
  protected lazy val jsonVulnerabilityGuard = false

  override protected def contentTypeInferrer = ({
    case _ : JValue => "application/json; charset=utf-8"
  }: ContentTypeInferrer) orElse super.contentTypeInferrer


  override protected def renderPipeline = ({
    case jv: JValue if format == "xml" =>
      contentType = "application/xml"
      toXml(jv).toString().getBytes(UTF8)
    case jv : JValue =>
      val jsonString = compact(render(jv))

      val jsonWithCallback = for {
        paramName <- jsonpCallbackParameterNames
        callback <- params.get(paramName)
      } yield "%s(%s);" format (callback, jsonString)

      val prelude = if (jsonVulnerabilityGuard && jsonWithCallback.isEmpty) ")]}',\n" else ""
      (jsonWithCallback.headOption.getOrElse(prelude+jsonString)).getBytes(UTF8)
  }: RenderPipeline) orElse super.renderPipeline
}
