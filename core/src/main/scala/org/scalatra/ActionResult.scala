package org.scalatra

case class ActionResult(
  status: ResponseStatus,
  body: Any,
  headers: Map[String, String])

private object ActionResultHelpers {

  def responseStatus(status: Int, reason: String): ResponseStatus = {
    reason match {
      case "" | null => ResponseStatus(status)
      case _ => new ResponseStatus(status, reason)
    }
  }

}

import org.scalatra.ActionResultHelpers._

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

object NonAuthoritativeInformation {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(203, reason), body, headers)
}

object NoContent {
  def apply(headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(204, reason), Unit, headers)
}

object ResetContent {
  def apply(headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(205, reason), Unit, headers)
}

object PartialContent {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(206, reason), body, headers)
}

object MultiStatus {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(207, reason), body, headers)
}

object AlreadyReported {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(208, reason), body, headers)
}

object IMUsed {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(226, reason), body, headers)
}

object MultipleChoices {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(300, reason), body, headers)
}

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

object UseProxy {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(305, reason), Unit, Map("Location" -> location) ++ headers)
}

object TemporaryRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(307, reason), Unit, Map("Location" -> location) ++ headers)
}

object PermanentRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(308, reason), Unit, Map("Location" -> location) ++ headers)
}

object BadRequest {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(400, reason), body, headers)
}

object Unauthorized {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(401, reason), body, headers)
}

object PaymentRequired {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(402, reason), body, headers)
}

object Forbidden {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(403, reason), body, headers)
}

object NotFound {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(404, reason), body, headers)
}

object MethodNotAllowed {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(405, reason), body, headers)
}

object NotAcceptable {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(406, reason), body, headers)
}

object ProxyAuthenticationRequired {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(407, reason), body, headers)
}

object RequestTimeout {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(408, reason), body, headers)
}

object Conflict {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(409, reason), body, headers)
}

object Gone {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(410, reason), body, headers)
}

object LengthRequired {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(411, reason), body, headers)
}

object PreconditionFailed {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(412, reason), body, headers)
}

object RequestEntityTooLarge {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(413, reason), body, headers)
}

object RequestURITooLong {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(414, reason), body, headers)
}

object UnsupportedMediaType {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(415, reason), body, headers)
}

object RequestedRangeNotSatisfiable {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(416, reason), body, headers)
}

object ExpectationFailed {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(417, reason), body, headers)
}

object UnprocessableEntity {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(422, reason), body, headers)
}

object Locked {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(423, reason), body, headers)
}

object FailedDependency {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(424, reason), body, headers)
}

object UpgradeRequired {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(426, reason), body, headers)
}

object PreconditionRequired {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(428, reason), body, headers)
}

object TooManyRequests {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(429, reason), body, headers)
}

object RequestHeaderFieldsTooLarge {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(431, reason), body, headers)
}

object InternalServerError {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(500, reason), body, headers)
}

object NotImplemented {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(501, reason), body, headers)
}

object BadGateway {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(502, reason), body, headers)
}

object ServiceUnavailable {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(503, reason), body, headers)
}

object GatewayTimeout {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(504, reason), body, headers)
}

object HTTPVersionNotSupported {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(505, reason), body, headers)
}

object VariantAlsoNegotiates {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(506, reason), body, headers)
}

object InsufficientStorage {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(507, reason), body, headers)
}

object LoopDetected {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(508, reason), body, headers)
}

object NotExtended {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(510, reason), body, headers)
}

object NetworkAuthenticationRequired {
  def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
    ActionResult(responseStatus(511, reason), body, headers)
}

