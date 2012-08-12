package org.scalatra
package jackson

import java.io.InputStream
import com.fasterxml.jackson.databind.node.MissingNode

trait JacksonSupport extends json.JsonSupport with JacksonOutput {
  protected def readJsonFromStream(stream: InputStream): JsonType =
      jsonMapper.readTree(stream)

    protected def readXmlFromStream(stream: InputStream): JsonType =
        xmlMapper.readTree(stream)

    protected val jsonZero: JsonType = MissingNode.getInstance()
}
