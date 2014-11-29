package org.scalatra
package json

import text.Document
import org.json4s._
import java.io.{InputStreamReader, InputStream, Writer}
import util.RicherString._

trait NativeJsonSupport extends JsonSupport[Document] with NativeJsonOutput with JValueResult {
  protected def readJsonFromStreamWithCharset(stream: InputStream, charset: String): JValue = {
    val rdr = new InputStreamReader(stream, charset)
    if (rdr.ready()) native.JsonParser.parse(rdr, jsonFormats.wantsBigDecimal)
    else {
      rdr.close()
      JNothing
    }
  }

  protected def readJsonFromBody(bd: String): JValue = {
    if (bd.nonBlank) native.JsonParser.parse(bd, jsonFormats.wantsBigDecimal)
    else JNothing
  }
}

trait NativeJsonValueReaderProperty extends JsonValueReaderProperty[Document] { self: native.JsonMethods => }


trait NativeJsonOutput extends JsonOutput[Document] with native.JsonMethods {
  protected def writeJson(json: JValue, writer: Writer) {
    if (json != JNothing) native.Printer.compact(render(json), writer)
  }
}


