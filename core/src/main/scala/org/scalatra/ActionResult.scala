package org.scalatra

case class ActionResult(
  status: Int,
  body: Any,
  headers: Map[String, String])

object Ok {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(200, body, headers)
}

object Created {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(201, body, headers)
}

object Accepted {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(202, body, headers)
}

object NonAuthoritativeInformation {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(203, body, headers)
}

object NoContent {
  def apply(headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(204, (), headers)
}

object ResetContent {
  def apply(headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(205, (), headers)
}

object PartialContent {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(206, body, headers)
}

object MultiStatus {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(207, body, headers)
}

object AlreadyReported {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(208, body, headers)
}

object IMUsed {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(226, body, headers)
}

object MultipleChoices {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(300, body, headers)
}

object MovedPermanently {
  def apply(location: String, headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(301, (), Map("Location" -> location) ++ headers)
}

object Found {
  def apply(location: String, headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(302, (), Map("Location" -> location) ++ headers)
}

object SeeOther {
  def apply(location: String, headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(303, (), Map("Location" -> location) ++ headers)
}

object NotModified {
  def apply(headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(304, (), headers)
}

object UseProxy {
  def apply(location: String, headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(305, (), Map("Location" -> location) ++ headers)
}

object TemporaryRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(307, (), Map("Location" -> location) ++ headers)
}

object PermanentRedirect {
  def apply(location: String, headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(308, (), Map("Location" -> location) ++ headers)
}

object BadRequest {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(400, body, headers)
}

object Unauthorized {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(401, body, headers)
}

object PaymentRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(402, body, headers)
}

object Forbidden {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(403, body, headers)
}

object NotFound {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(404, body, headers)
}

object MethodNotAllowed {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(405, body, headers)
}

object NotAcceptable {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(406, body, headers)
}

object ProxyAuthenticationRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(407, body, headers)
}

object RequestTimeout {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(408, body, headers)
}

object Conflict {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(409, body, headers)
}

object Gone {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(410, body, headers)
}

object LengthRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(411, body, headers)
}

object PreconditionFailed {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(412, body, headers)
}

object RequestEntityTooLarge {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(413, body, headers)
}

object RequestURITooLong {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(414, body, headers)
}

object UnsupportedMediaType {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(415, body, headers)
}

object RequestedRangeNotSatisfiable {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(416, body, headers)
}

object ExpectationFailed {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(417, body, headers)
}

object UnprocessableEntity {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(422, body, headers)
}

object Locked {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(423, body, headers)
}

object FailedDependency {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(424, body, headers)
}

object UpgradeRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(426, body, headers)
}

object PreconditionRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(428, body, headers)
}

object TooManyRequests {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(429, body, headers)
}

object RequestHeaderFieldsTooLarge {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(431, body, headers)
}

object InternalServerError {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(500, body, headers)
}

object NotImplemented {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(501, body, headers)
}

object BadGateway {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(502, body, headers)
}

object ServiceUnavailable {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(503, body, headers)
}

object GatewayTimeout {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(504, body, headers)
}

object HTTPVersionNotSupported {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(505, body, headers)
}

object VariantAlsoNegotiates {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(506, body, headers)
}

object InsufficientStorage {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(507, body, headers)
}

object LoopDetected {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(508, body, headers)
}

object NotExtended {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(510, body, headers)
}

object NetworkAuthenticationRequired {
  def apply(body: Any = (), headers: Map[String, String] = Map.empty): ActionResult =
    ActionResult(511, body, headers)
}

