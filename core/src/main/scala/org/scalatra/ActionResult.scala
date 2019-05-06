package org.scalatra

case class ActionResult(
  status: Int,
  body: Any,
  headers: Map[String, String])

object Ok {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(200, body, headers)
}

object Created {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(201, body, headers)
}

object Accepted {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(202, body, headers)
}

object NonAuthoritativeInformation {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(203, body, headers)
}

object NoContent {
  def apply(headers: Map[String, String] = Map.empty) =
    ActionResult(204, (), headers)
}

object ResetContent {
  def apply(headers: Map[String, String] = Map.empty) =
    ActionResult(205, (), headers)
}

object PartialContent {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(206, body, headers)
}

object MultiStatus {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(207, body, headers)
}

object AlreadyReported {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(208, body, headers)
}

object IMUsed {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(226, body, headers)
}

object MultipleChoices {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(300, body, headers)
}

object MovedPermanently {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(301, (), Map("Location" -> location) ++ headers)
}

object Found {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(302, (), Map("Location" -> location) ++ headers)
}

object SeeOther {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(303, (), Map("Location" -> location) ++ headers)
}

object NotModified {
  def apply(headers: Map[String, String] = Map.empty) =
    ActionResult(304, (), headers)
}

object UseProxy {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(305, (), Map("Location" -> location) ++ headers)
}

object TemporaryRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(307, (), Map("Location" -> location) ++ headers)
}

object PermanentRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty) =
    ActionResult(308, (), Map("Location" -> location) ++ headers)
}

object BadRequest {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(400, body, headers)
}

object Unauthorized {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(401, body, headers)
}

object PaymentRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(402, body, headers)
}

object Forbidden {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(403, body, headers)
}

object NotFound {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(404, body, headers)
}

object MethodNotAllowed {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(405, body, headers)
}

object NotAcceptable {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(406, body, headers)
}

object ProxyAuthenticationRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(407, body, headers)
}

object RequestTimeout {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(408, body, headers)
}

object Conflict {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(409, body, headers)
}

object Gone {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(410, body, headers)
}

object LengthRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(411, body, headers)
}

object PreconditionFailed {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(412, body, headers)
}

object RequestEntityTooLarge {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(413, body, headers)
}

object RequestURITooLong {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(414, body, headers)
}

object UnsupportedMediaType {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(415, body, headers)
}

object RequestedRangeNotSatisfiable {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(416, body, headers)
}

object ExpectationFailed {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(417, body, headers)
}

object UnprocessableEntity {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(422, body, headers)
}

object Locked {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(423, body, headers)
}

object FailedDependency {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(424, body, headers)
}

object UpgradeRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(426, body, headers)
}

object PreconditionRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(428, body, headers)
}

object TooManyRequests {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(429, body, headers)
}

object RequestHeaderFieldsTooLarge {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(431, body, headers)
}

object InternalServerError {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(500, body, headers)
}

object NotImplemented {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(501, body, headers)
}

object BadGateway {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(502, body, headers)
}

object ServiceUnavailable {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(503, body, headers)
}

object GatewayTimeout {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(504, body, headers)
}

object HTTPVersionNotSupported {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(505, body, headers)
}

object VariantAlsoNegotiates {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(506, body, headers)
}

object InsufficientStorage {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(507, body, headers)
}

object LoopDetected {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(508, body, headers)
}

object NotExtended {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(510, body, headers)
}

object NetworkAuthenticationRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty) =
    ActionResult(511, body, headers)
}

