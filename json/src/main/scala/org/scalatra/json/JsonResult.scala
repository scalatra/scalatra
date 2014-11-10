package org.scalatra.json

import org.json4s.JValue

case class JsonResult(value: JValue)

object JsonResult {
  def apply[T <% JValue](v: T): JsonResult = JsonResult(v: JValue)
}
