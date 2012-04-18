package org.scalatra

case class ActionResult(val status: ResponseStatus, val body: Any, val headers: Map[String, String])

private object Helpers {
  def responseStatus(status: Int, reason: String) = reason match {
    case "" => ResponseStatus(status)
    case _  => new ResponseStatus(status, reason) 
  }
}

import Helpers._

// 200 success

object Ok {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(200, reason), body, headers)
}

object Created {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(201, reason), body, headers)
}

object Accepted {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(202, reason), body, headers)
}

object NoContent {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(204, reason), body, headers)
}

// 300 Redirection

object MovedPermanently {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(301, reason), Unit, Map("Location" -> location) ++ headers)
}

object Found {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(302, reason), Unit, Map("Location" -> location) ++ headers)
}

object SeeOther {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(303, reason), Unit, Map("Location" -> location) ++ headers)
}

object NotModified {
  def apply(headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(304, reason), Unit, headers)
}

object TemporaryRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(307, reason), Unit, Map("Location" -> location) ++ headers)
}

// 400 Client Error

object BadRequest {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(400, reason), body, headers)
}

object Unauthorized {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(401, reason), body, headers)
}

object Forbidden {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(403, reason), body, headers)
}

object NotFound {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(404, reason), body, headers)
}

object NotAcceptable {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(406, reason), body, headers)
}

object Gone {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(410, reason), body, headers)
}

object RequestEntityTooLarge {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(413, reason), body, headers)
}

object UnsupportedMediaType {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(415, reason), body, headers)
}

object UnprocessableEntity {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(422, reason), body, headers)
}

object Locked {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(423, reason), body, headers)
}

object TooManyRequests {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(429, reason), body, headers)
}
