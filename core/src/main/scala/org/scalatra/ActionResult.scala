package org.scalatra

case class ActionResult(val status: Int, val body: Any, val headers: Map[String, String])

object Ok {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(200, body, headers)
}

object BadRequest {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(400, body, headers)
}
