package org.scalatra
package json

import java.io.Writer

import org.json4s.*
import native.*
import org.scalatra.util.RicherString.*

trait NativeJsonSupport extends JsonSupport[Document] with NativeJsonOutput with JValueResult {
  protected def readJsonFromBody(bd: String): JValue = {
    if (bd.nonBlank) native.JsonParser.parse(bd, jsonFormats.wantsBigDecimal)
    else JNothing
  }
}

trait NativeJsonOutput extends JsonOutput[Document] with native.JsonMethods {
  protected def writeJson(json: JValue, writer: Writer): Unit = {
    if (json != JNothing) native.Printer.compact(render(json), writer)
  }
}
