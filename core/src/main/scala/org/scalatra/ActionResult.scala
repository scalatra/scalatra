package org.scalatra

case class ActionResult(val status: Int, val body: Any, val headers: Map[String, String])

// 200 success

object Ok {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(200, body, headers)
}

object Created {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(201, body, headers)
}

object Accepted {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(202, body, headers)
}

object NoContent {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(204, body, headers)
}

// 300 Redirection

object MovedPermanently {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(301, Unit, Map("Location" -> location) ++ headers)
}

object Found {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(302, Unit, Map("Location" -> location) ++ headers)
}

object SeeOther {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(303, Unit, Map("Location" -> location) ++ headers)
}

object NotModified {
  def apply(headers: Map[String, String] = Map.empty) =
    ActionResult(304, Unit, headers)
}

object TemporaryRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(307, Unit, Map("Location" -> location) ++ headers)
}

// 400 Client Error

object BadRequest {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(400, body, headers)
}

object Unauthorized {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(401, body, headers)
}

object Forbidden {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(403, body, headers)
}

object NotFound {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(404, body, headers)
}

object NotAcceptable {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(406, body, headers)
}

object Gone {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(410, body, headers)
}

object RequestEntityTooLarge {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(413, body, headers)
}

object UnsupportedMediaType {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(415, body, headers)
}

object UnprocessableEntity {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(422, body, headers)
}

object Locked {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(423, body, headers)
}

object TooManyRequests {
  def apply(body: Any, headers: Map[String, String] = Map.empty) =
    ActionResult(429, body, headers)
}
