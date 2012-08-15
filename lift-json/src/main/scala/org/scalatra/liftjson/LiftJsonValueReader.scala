package org.scalatra
package liftjson

import net.liftweb.json._
import json.{JsonValueReaderProperty, JsonValueReader}

class LiftJsonValueReader(data: JValue)(implicit formats: Formats) extends JsonValueReader(data) {
  def get(key: String, subj: JValue): Option[JValue] = subj \ key match {
    case JNull | JNothing => None
    case o => Some(o)
  }
}


trait LiftJsonValueReaderProperty extends JsonValueReaderProperty {

  type JsonType = JValue

  implicit protected def jsonFormats: Formats
  protected implicit def jsonValueReader(d: JValue): JsonValueReader[JValue] = new LiftJsonValueReader(d)
}

class LiftJsonValueReaderImports(implicit protected val jsonFormats: Formats) extends LiftJsonValueReaderProperty