package org.scalatra
package liftjson

import net.liftweb.json._
import net.liftweb.json.Xml._
import java.io.{InputStream, InputStreamReader}


@deprecated("Use LiftJsonSupportWithoutFormats instead", "2.1.0")
trait LiftJsonRequestBodyWithoutFormats extends LiftJsonSupportWithoutFormats

@deprecated("Use LiftJsonSupport instead", "2.1.0")
trait LiftJsonRequestBody extends LiftJsonSupport

trait LiftJsonSupportWithoutFormats extends LiftJsonOutput with json.JsonSupport {


  protected def readJsonFromStream(stream: InputStream): JsonType =
    JsonParser.parse(new InputStreamReader(stream))

  protected def readXmlFromStream(stream: InputStream): JsonType =
    toJson(scala.xml.XML.load(stream))

  protected val jsonZero: JsonType = JNothing

}

/**
 * Parses request bodies with lift json if the appropriate content type is set.
 * Be aware that it also parses XML and returns a JValue of the parsed XML.
 */
trait LiftJsonSupport extends LiftJsonSupportWithoutFormats {

  implicit def jsonFormats: Formats = DefaultFormats

  

}