package org.scalatra
package json

import java.io.{ InputStream, InputStreamReader, Writer }

import org.json4s._
import native._
import org.scalatra.util.RicherString._

trait NativeJsonSupport extends JsonSupport[Document] with NativeJsonOutput with JValueResult {
  protected def readJsonFromBody(bd: String): JValue = {
    if (bd.nonBlank) native.JsonParser.parse(bd, jsonFormats.wantsBigDecimal)
    else JNothing
  }
}

trait NativeJsonValueReaderProperty extends JsonValueReaderProperty[Document] { self: native.JsonMethods => }

trait NativeJsonOutput extends JsonOutput[Document] with native.JsonMethods {
  protected def writeJson(json: JValue, writer: Writer): Unit = {
    if (json != JNothing) native.Printer.compact(render(json), writer)
  }
}

