package org.scalatra
package jerkson

import _root_.com.codahale.jerkson._
import io.Codec._
import AST._
import java.io.InputStream

object JerksonSupport {

  val ParsedBodyKey = "org.scalatra.jerkson.ParsedBody"
}

trait JerksonSupport extends ApiFormats {

  import JerksonSupport._

  /**
   * If a request is made with a parameter in jsonpCallbackParameterNames it will
   * be assumed that it is a JSONP request and the json will be returned as the
   * argument to a function with the name specified in the corresponding parameter.
   *
   * By default no parameterNames will be checked
   */
  def jsonpCallbackParameterNames:  Iterable[String] = Nil

  override protected def contentTypeInferrer = ({
    case _ : JValue => "application/json; charset=utf-8"
  }: ContentTypeInferrer) orElse super.contentTypeInferrer

  protected def renderJson(jv: JValue): String = Json.generate(jv)
  protected def parseJson(inputStream: InputStream): JValue = Json.parse[JValue](inputStream)
//  protected def parseXml(inputStream: InputStream): JValue = JUndefined

  override protected def renderPipeline = ({
    case jv : JValue =>
      val jsonString = renderJson(jv)

      val jsonWithCallback = for {
        paramName <- jsonpCallbackParameterNames
        callback <- params.get(paramName)
      } yield "%s(%s);" format (callback, jsonString)

      (jsonWithCallback.headOption.getOrElse(jsonString)).getBytes(UTF8)
  }: RenderPipeline) orElse super.renderPipeline

  protected def parseRequestBody(format: String) = try {
      if (format == "json") {
        transformRequestBody(parseJson(request.inputStream))
      } else JUndefined
    } catch {
      case _ ⇒ JUndefined
    }

    protected def transformRequestBody(body: JValue) = body

    override protected def invoke(matchedRoute: MatchedRoute) = {
      withRouteMultiParams(Some(matchedRoute)) {
        val mt = request.contentType map {
          _.split(";").head
        } getOrElse "application/x-www-form-urlencoded"
        val fmt = mimeTypes get mt getOrElse "html"
        if (shouldParseBody(fmt)) {
          request(ParsedBodyKey) = parseRequestBody(fmt)
        }
        super.invoke(matchedRoute)
      }
    }

    private def shouldParseBody(fmt: String) =
      (fmt == "json") && parsedBody == JUndefined

    def parsedBody = request.get(ParsedBodyKey) getOrElse JUndefined
}
