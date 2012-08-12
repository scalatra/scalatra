package org.scalatra
package jackson

import java.io.InputStream
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper

trait JacksonSupport extends json.JsonSupport with JacksonOutput {
  protected def readJsonFromStream(stream: InputStream): JsonType =
      jsonMapper.readTree(stream)

    protected def readXmlFromStream(stream: InputStream): JsonType =
      try {

        xmlMapper.readTree(stream)
      } catch {
        case e: Throwable =>
          e.printStackTrace()
          throw e
      }

    protected val jsonZero: JsonType = MissingNode.getInstance()
}
