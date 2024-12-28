package org.scalatra.json

import org.json4s.JValue

case class JsonResult(value: JValue)

object JsonResult {
  def apply[T](v: T)(implicit T: T => JValue): JsonResult = JsonResult(
    T.apply(v)
  )
}
