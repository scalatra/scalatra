package org.scalatra
package json

import javax.servlet.http.HttpServletRequest

import org.json4s.Xml._
import org.json4s._
import org.scalatra.util.RicherString._
import org.slf4j.LoggerFactory

import javax.xml.parsers.SAXParserFactory
import scala.xml.{ Elem, XML }
import scala.xml.factory.XMLLoader

object JsonSupport {

  val ParsedBodyKey = "org.scalatra.json.ParsedBody"
}

trait JsonSupport[T] extends JsonOutput[T] {

  import org.scalatra.json.JsonSupport._

  private[this] val logger = LoggerFactory.getLogger(getClass)

  protected def parseRequestBody(format: String)(implicit request: HttpServletRequest) = try {
    val ct = request.contentType getOrElse ""
    if (format == "json") {
      if (ct == "application/x-www-form-urlencoded") multiParams.keys.headOption map readJsonFromBody getOrElse JNothing
      else readJsonFromBody(request.body)
    } else if (format == "xml") {
      if (ct == "application/x-www-form-urlencoded") multiParams.keys.headOption map readXmlFromBody getOrElse JNothing
      else readXmlFromBody(request.body)
    } else JNothing
  } catch {
    case t: Throwable ⇒ {
      logger.error(s"Parsing the request body failed, because:", t)
      JNothing
    }
  }

  protected def readJsonFromBody(bd: String): JValue

  def secureXML: XMLLoader[Elem] = {
    val parserFactory = SAXParserFactory.newInstance()
    parserFactory.setNamespaceAware(false)
    parserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    parserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    val saxParser = parserFactory.newSAXParser()
    XML.withSAXParser(saxParser)
  }

  protected def readXmlFromBody(bd: String): JValue = {
    if (bd.nonBlank) {
      val JObject(JField(_, jv) :: Nil) = toJson(secureXML.loadString(bd))
      jv
    } else JNothing
  }

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
