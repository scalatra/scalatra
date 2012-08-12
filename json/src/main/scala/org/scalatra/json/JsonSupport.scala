package org.scalatra
package json

import java.io.InputStream

object JsonSupport {

  val ParsedBodyKey = "org.scalatra.json.ParsedBody"
}
trait JsonSupport extends JsonOutput {

  import JsonSupport._

  protected def parseRequestBody(format: String) = try {
    if (format == "json") {
      transformRequestBody(readJsonFromStream(request.inputStream))
    } else if (format == "xml") {
      transformRequestBody(readXmlFromStream(request.inputStream))
    } else jsonZero
  } catch {
    case _: Throwable ⇒ jsonZero
  }

  protected def readJsonFromStream(stream: InputStream): JsonType
  protected def readXmlFromStream(stream: InputStream): JsonType
  protected def jsonZero: JsonType
  protected def transformRequestBody(body: JsonType) = body

  override protected def invoke(matchedRoute: MatchedRoute) = {
    withRouteMultiParams(Some(matchedRoute)) {
      val mt = request.contentType map {
        _.split(";").head
      } getOrElse "application/x-www-form-urlencoded"
      val fmt = mimeTypes get mt getOrElse "html"
      if (shouldParseBody(fmt)) {
        request(ParsedBodyKey) = parseRequestBody(fmt).asInstanceOf[AnyRef]
      }
      super.invoke(matchedRoute)
    }
  }

  private def shouldParseBody(fmt: String) =
    (fmt == "json" || fmt == "xml") && parsedBody == jsonZero

  def parsedBody: JsonType = request.get(ParsedBodyKey).map(_.asInstanceOf[JsonType]) getOrElse {
    val fmt = format
    var bd: JsonType = jsonZero
    if (fmt == "json" || fmt == "xml") {
      bd = parseRequestBody(fmt)
      request(ParsedBodyKey) = bd.asInstanceOf[AnyRef]
    }
    bd
  }
}