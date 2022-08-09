package org.scalatra
package json

import jakarta.servlet.http.HttpServletRequest
import javax.xml.XMLConstants

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
    if (format == "json") {
      transformRequestBody(readJsonFromBody(request.body))
    } else if (format == "xml") {
      transformRequestBody(readXmlFromBody(request.body))
    } else JNothing
  } catch {
    case t: Throwable => {
      logger.error(s"Parsing the request body failed, because:", t)
      JNothing
    }
  }

  protected def readJsonFromBody(bd: String): JValue

  def secureXML: XMLLoader[Elem] = {
    val parserFactory = SAXParserFactory.newInstance()
    parserFactory.setNamespaceAware(false)
    parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
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

  protected def transformRequestBody(body: JValue) = body

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
