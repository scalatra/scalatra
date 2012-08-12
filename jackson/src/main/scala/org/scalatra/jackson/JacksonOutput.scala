package org.scalatra
package jackson

import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper, JsonNode}
import java.io.{Writer, PrintWriter}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.dataformat.xml.{XmlFactory, XmlMapper}
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.databind.node.{ContainerNode, JsonNodeFactory, ObjectNode}
import collection.mutable
import collection.JavaConverters._
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.util
import com.fasterxml.jackson.annotation.JsonUnwrapped
import xml.XML

private[jackson] trait JacksonOutput extends json.JsonOutput {

  protected val jsonMapper = new ObjectMapper()
  protected val xmlMapper = new XmlMapper()
  configureJackson(jsonMapper)
  configureJackson(xmlMapper)

  protected def configureJackson(mapper: ObjectMapper) {
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(SerializationFeature.CLOSE_CLOSEABLE, false)
    mapper.configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false)
  }

  protected type JsonType = JsonNode

  protected def jsonClass: Class[_] = classOf[JsonType]

  protected def writeJsonAsXml(json: JsonType, writer: Writer) {
    val nodes = xmlRootNode.copy(child = XML.loadString(xmlMapper.writeValueAsString(json)).child)
    XML.write(response.writer, xml.Utility.trim(nodes), response.characterEncoding.get, xmlDecl = true, null)
  }

  protected def writeJson(json: JsonType, writer: Writer) {
    writer write jsonMapper.writeValueAsString(json)
  }
}
