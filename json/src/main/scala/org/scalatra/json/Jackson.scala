package org.scalatra
package json

import java.io.{Writer, InputStreamReader, InputStream}
import org.json4s._

trait JacksonJsonSupport extends JsonSupport[JValue] with JacksonJsonOutput with JValueResult {
  protected def readJsonFromStreamWithCharset(stream: InputStream, charset: String): JValue =
    mapper.readValue(new InputStreamReader(stream, charset), classOf[JValue])

  protected def readJsonFromBody(bd: String): JValue = mapper.readValue(bd, classOf[JValue])
}

trait JacksonJsonValueReaderProperty extends JsonValueReaderProperty[JValue] { self: jackson.JsonMethods => }


trait JacksonJsonOutput extends JsonOutput[JValue] with jackson.JsonMethods {
  protected def writeJson(json: JValue, writer: Writer) {
    mapper.writeValue(writer, json)
  }
}
