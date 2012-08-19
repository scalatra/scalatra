package org.scalatra
package liftjson

import net.liftweb.json._
import json.{JsonTypeAlias, JsonValueReaderProperty, JsonValueReader}

class LiftJsonValueReader(data: JValue)(implicit formats: Formats) extends JsonValueReader(data) {
//  implicit val manifest: Manifest[I] = Predef.manifest[JValue]
  def get(key: String, subj: JValue): Option[JValue] = {
    val jv = subj \ key
    jv match {
      case JNull | JNothing => None
      case o => Some(o)
    }
  }
}


trait LiftJsonValueReaderProperty extends JsonValueReaderProperty { self: JsonTypeAlias =>

  implicit protected def jsonFormats: Formats
  protected implicit def jsonValueReader(d: JValue): JsonValueReader[JValue] = new LiftJsonValueReader(d)
}

class LiftJsonValueReaderImports(implicit protected val jsonFormats: Formats) extends JsonTypeAlias with LiftJsonValueReaderProperty {
  protected type JsonType = JValue
}