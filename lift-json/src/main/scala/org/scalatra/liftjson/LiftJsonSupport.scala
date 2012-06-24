package org.scalatra
package liftjson

import net.liftweb.json._
import net.liftweb.json.Xml._
import java.io.InputStreamReader
import scala.io.Codec.UTF8

object LiftJsonSupport {

  val ParsedBodyKey = "org.scalatra.liftjson.ParsedBody".intern
}

trait LiftJsonSupportWithoutFormats extends ScalatraBase with ApiFormats {
  import LiftJsonSupport._

  /**
   * If a request is made with a parameter in jsonpCallbackParameterNames it will
   * be assumed that it is a JSONP request and the json will be returned as the
   * argument to a function with the name specified in the corresponding parameter.
   *
   * By default no parameterNames will be checked
   */
  def jsonpCallbackParameterNames:  Iterable[String] = None

  override protected def contentTypeInferrer = ({
    case _ : JValue => "application/json; charset="+(request.characterEncoding getOrElse defaultCharacterEncoding).toLowerCase
  }: ContentTypeInferrer) orElse super.contentTypeInferrer

  protected def parseRequestBody(format: String) = try {
    if (format == "json") {
      transformRequestBody(JsonParser.parse(new InputStreamReader(request.inputStream)))
    } else if (format == "xml") {
      transformRequestBody(toJson(scala.xml.XML.load(request.inputStream)))
    } else JNothing
  } catch {
    case _ ⇒ JNothing
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
    (fmt == "json" || fmt == "xml") && parsedBody == JNothing

  def parsedBody = request.get(ParsedBodyKey) getOrElse JNothing

  override protected def renderPipeline = ({
    case jv : JValue =>
      val jsonString = compact(render(jv))

      val jsonWithCallback = for {
        paramName <- jsonpCallbackParameterNames
        callback <- params.get(paramName)
      } yield "%s(%s);" format (callback, jsonString)

      (jsonWithCallback.headOption.getOrElse(jsonString)).getBytes(UTF8)
  }: RenderPipeline) orElse super.renderPipeline
}

/**
 * Parses request bodies with lift json if the appropriate content type is set.
 * Be aware that it also parses XML and returns a JValue of the parsed XML.
 */
trait LiftJsonSupport extends LiftJsonSupportWithoutFormats {

  protected implicit def jsonFormats: Formats = DefaultFormats

  

}