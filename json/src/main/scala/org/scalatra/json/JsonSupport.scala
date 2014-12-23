package org.scalatra
package json

import java.io.{ InputStream, InputStreamReader }
import javax.servlet.http.HttpServletRequest

import org.json4s.Xml._
import org.json4s._
import org.scalatra.util.RicherString._
import org.slf4j.LoggerFactory

object JsonSupport {

  val ParsedBodyKey = "org.scalatra.json.ParsedBody"
}

trait JsonSupport[T] extends JsonOutput[T] {

  import org.scalatra.json.JsonSupport._

  private[this] val logger = LoggerFactory.getLogger(getClass)

  private[this] val _defaultCacheRequestBody = true
  protected def cacheRequestBodyAsString: Boolean = _defaultCacheRequestBody
  protected def parseRequestBody(format: String)(implicit request: HttpServletRequest) = try {
    val ct = request.contentType getOrElse ""
    if (format == "json") {
      val bd = {
        if (ct == "application/x-www-form-urlencoded") multiParams.keys.headOption map readJsonFromBody getOrElse JNothing
        else if (cacheRequestBodyAsString) readJsonFromBody(request.body)
        else readJsonFromStreamWithCharset(request.inputStream, request.characterEncoding getOrElse defaultCharacterEncoding)
      }
      transformRequestBody(bd)
    } else if (format == "xml") {
      val bd = {
        if (ct == "application/x-www-form-urlencoded") multiParams.keys.headOption map readXmlFromBody getOrElse JNothing
        else if (cacheRequestBodyAsString) readXmlFromBody(request.body)
        else readXmlFromStream(request.inputStream)
      }
      transformRequestBody(bd)
    } else JNothing
  } catch {
    case t: Throwable ⇒ {
      logger.error(s"Parsing the request body failed, because:", t)
      JNothing
    }
  }

  protected def readJsonFromBody(bd: String): JValue
  protected def readJsonFromStreamWithCharset(stream: InputStream, charset: String): JValue
  protected def readJsonFromStream(stream: InputStream): JValue = readJsonFromStreamWithCharset(stream, defaultCharacterEncoding)
  protected def readXmlFromBody(bd: String): JValue = {
    if (bd.nonBlank) {
      val JObject(JField(_, jv) :: Nil) = toJson(scala.xml.XML.loadString(bd))
      jv
    } else JNothing
  }
  protected def readXmlFromStream(stream: InputStream): JValue = {
    val rdr = new InputStreamReader(stream)
    if (rdr.ready()) {
      val JObject(JField(_, jv) :: Nil) = toJson(scala.xml.XML.load(rdr))
      jv
    } else JNothing
  }
  protected def transformRequestBody(body: JValue) = body

  override protected def invoke(matchedRoute: MatchedRoute) = {
    withRouteMultiParams(Some(matchedRoute)) {
      val mt = request.contentType.fold("application/x-www-form-urlencoded")(_.split(";").head)
      val fmt = mimeTypes get mt getOrElse "html"
      if (shouldParseBody(fmt)) {
        request(ParsedBodyKey) = parseRequestBody(fmt).asInstanceOf[AnyRef]
      }
      super.invoke(matchedRoute)
    }
  }

  protected def shouldParseBody(fmt: String)(implicit request: HttpServletRequest) =
    (fmt == "json" || fmt == "xml") && !request.requestMethod.isSafe && parsedBody == JNothing

  def parsedBody(implicit request: HttpServletRequest): JValue = request.get(ParsedBodyKey).fold({
    val fmt = requestFormat
    var bd: JValue = JNothing
    if (fmt == "json" || fmt == "xml") {
      bd = parseRequestBody(fmt)
      request(ParsedBodyKey) = bd.asInstanceOf[AnyRef]
    }
    bd
  })(_.asInstanceOf[JValue])
}
