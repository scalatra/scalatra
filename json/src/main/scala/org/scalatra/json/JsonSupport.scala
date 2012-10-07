package org.scalatra
package json

import java.io.{InputStreamReader, InputStream}
import org.json4s._
import Xml._
import text.Document

object JsonSupport {

  val ParsedBodyKey = "org.scalatra.json.ParsedBody"
}

trait JsonSupport[T] extends JsonOutput[T] {

  import JsonSupport._

  private[this] val _defaultCacheRequestBody = true
  protected def cacheRequestBodyAsString: Boolean = _defaultCacheRequestBody
  protected def parseRequestBody(format: String) = try {
    if (format == "json") {
      val bd  = if (cacheRequestBodyAsString) readJsonFromBody(request.body) else readJsonFromStream(request.inputStream)
      transformRequestBody(bd)
    } else if (format == "xml") {
      val bd  = if (cacheRequestBodyAsString) readXmlFromBody(request.body) else readXmlFromStream(request.inputStream)
      transformRequestBody(bd)
    } else JNothing
  } catch {
    case _: Throwable ⇒ JNothing
  }

  protected def readJsonFromBody(bd: String): JValue
  protected def readJsonFromStream(stream: InputStream): JValue
  protected def readXmlFromBody(bd: String): JValue = {
    val JObject(JField(_, jv) :: Nil) = toJson(scala.xml.XML.loadString(bd))
    jv
  }
  protected def readXmlFromStream(stream: InputStream): JValue = {
    val JObject(JField(_, jv) :: Nil) = toJson(scala.xml.XML.load(stream))
    jv
  }
  protected def transformRequestBody(body: JValue) = body


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
    (fmt == "json" || fmt == "xml") && parsedBody == JNothing

  def parsedBody: JValue = request.get(ParsedBodyKey).map(_.asInstanceOf[JValue]) getOrElse {
    val fmt = format
    var bd: JValue = JNothing
    if (fmt == "json" || fmt == "xml") {
      bd = parseRequestBody(fmt)
      request(ParsedBodyKey) = bd.asInstanceOf[AnyRef]
    }
    bd
  }
}
