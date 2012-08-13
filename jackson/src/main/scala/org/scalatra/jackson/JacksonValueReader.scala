package org.scalatra
package jackson

import com.fasterxml.jackson.databind.JsonNode
import json.{JsonValueReaderProperty, JsonValueReader}
import com.fasterxml.jackson.databind.node.{NullNode, MissingNode, ObjectNode}

class JacksonValueReader(data: JsonNode) extends JsonValueReader(data) {


  protected def get(path: String, subj: JsonNode): Option[JsonNode] = subj match {
    case o: ObjectNode => o.get(path) match {
      case _: MissingNode | _: NullNode => None
      case v: JsonNode => Option(v)
    }
    case _ => None
  }
}

trait JacksonValueReaderProperty extends JsonValueReaderProperty { self: JacksonSupport =>
  protected implicit def jsonValueReader(d: JsonNode): JsonValueReader[JsonNode] = new JacksonValueReader(d)
}