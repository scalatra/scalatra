package org.scalatra
package jackson

import com.fasterxml.jackson.databind.{MapperFeature, SerializationFeature, ObjectMapper, JsonNode}
import java.io.Writer
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import xml.XML
import com.fasterxml.jackson.core.JsonGenerator

private[jackson] trait JacksonOutput extends json.JsonOutput {

  protected val jsonMapper = new ObjectMapper()
  val xmlMapper = new XmlMapper()
  configureJackson(jsonMapper)
  configureJackson(xmlMapper)

  protected def configureJackson(mapper: ObjectMapper) {
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
  }

  protected type JsonType = JsonNode

  protected def jsonClass: Class[_] = classOf[JsonType]

  protected def writeJsonAsXml(json: JsonType, writer: Writer) {
    val nodes = xmlRootNode.copy(child = XML.loadString(xmlMapper.writeValueAsString(json)).child)
    XML.write(response.writer, xml.Utility.trim(nodes), response.characterEncoding.get, xmlDecl = true, null)
  }

  protected def writeJson(json: JsonType, writer: Writer) {
    jsonMapper.writeValue(writer, json)
  }
}
