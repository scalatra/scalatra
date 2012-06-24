package org.scalatra
package liftjson

import net.liftweb.json._
import net.liftweb.json.Xml._
import java.io.InputStreamReader


object LiftJsonSupport {

  val ParsedBodyKey = "org.scalatra.liftjson.ParsedBody".intern
}

@deprecated("Use LiftJsonSupportWithoutFormats instead", "2.1.0")
trait LiftJsonRequestBodyWithoutFormats extends LiftJsonSupportWithoutFormats

@deprecated("Use LiftJsonSupport instead", "2.1.0")
trait LiftJsonRequestBody extends LiftJsonSupport

trait LiftJsonSupportWithoutFormats extends LiftJsonOutput {
  import LiftJsonSupport._


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


}

/**
 * Parses request bodies with lift json if the appropriate content type is set.
 * Be aware that it also parses XML and returns a JValue of the parsed XML.
 */
trait LiftJsonSupport extends LiftJsonSupportWithoutFormats {

  protected implicit def jsonFormats: Formats = DefaultFormats

  

}