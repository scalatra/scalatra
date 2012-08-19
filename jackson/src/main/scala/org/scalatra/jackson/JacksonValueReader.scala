package org.scalatra
package jackson

import com.fasterxml.jackson.databind.JsonNode
import json.{JsonTypeAlias, JsonValueReaderProperty, JsonValueReader}
import com.fasterxml.jackson.databind.node.{NullNode, MissingNode, ObjectNode}

class JacksonValueReader(data: JsonNode) extends JsonValueReader(data) {
//  implicit val manifest: Manifest[I] = Predef.manifest[JsonNode]

  protected def get(path: String, subj: JsonNode): Option[JsonNode] = subj match {
    case o: ObjectNode => { o.get(path) match {
        case _: MissingNode | _: NullNode => None
        case v: JsonNode => Option(v)
      }
    }
    case o => {
      println("unknown node")
      None
    }
  }
}

trait JacksonValueReaderProperty extends JsonValueReaderProperty { this: JsonTypeAlias =>
  protected implicit def jsonValueReader(d: JsonNode): JsonValueReader[JsonNode] = new JacksonValueReader(d)
}
object JacksonValueReaderProperty extends JsonTypeAlias with JacksonValueReaderProperty {
  protected type JsonType = JsonNode
}