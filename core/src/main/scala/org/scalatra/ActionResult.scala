package org.scalatra

case class ActionResult(
  status: ResponseStatus,
  body: Any,
  headers: Map[String, String])

object ActionResult
    extends ((ResponseStatus, Any, Map[String, String]) => ActionResult) { // SI-3664
  sealed abstract class Contentful(statusCode: Int)
      extends ((Any, Map[String, String], String) => ActionResult) {
    def apply(body: Any = Unit, headers: Map[String, String] = Map.empty, reason: String = "") =
      ActionResult(responseStatus(statusCode, reason), body, headers)
  }

  sealed abstract class Contentless(statusCode: Int)
      extends ((Map[String, String], String) => ActionResult) {
    def apply(headers: Map[String, String] = Map.empty, reason: String = "") =
      ActionResult(responseStatus(statusCode, reason), Unit, headers)
  }

  sealed abstract class WithLocation(statusCode: Int)
      extends ((String, Map[String, String], String) => ActionResult) {
    def apply(location: String, headers: Map[String, String] = Map.empty, reason: String = "") =
      ActionResult(responseStatus(statusCode, reason), Unit, Map("Location" -> location) ++ headers)
  }

  private[this] def responseStatus(status: Int, reason: String): ResponseStatus = {
    reason match {
      case "" | null => ResponseStatus(status)
      case _ => new ResponseStatus(status, reason)
    }
  }
}

import ActionResult._

object Ok extends Contentful(200)

object Created extends Contentful(201)

object Accepted extends Contentful(202)

object NonAuthoritativeInformation extends Contentful(203)

object NoContent extends Contentless(204)

object ResetContent extends Contentless(205)

object PartialContent extends Contentful(206)

object MultiStatus extends Contentful(207)

object AlreadyReported extends Contentful(208)

object IMUsed extends Contentful(226)

object MultipleChoices extends Contentful(300)

object MovedPermanently extends WithLocation(301)

object Found extends WithLocation(302)

object SeeOther extends WithLocation(303)

object NotModified extends Contentless(304)

object UseProxy extends WithLocation(305)

object TemporaryRedirect extends WithLocation(307)

object PermanentRedirect extends WithLocation(308)

object BadRequest extends Contentful(400)

object Unauthorized extends Contentful(401)

object PaymentRequired extends Contentful(402)

object Forbidden extends Contentful(403)

object NotFound extends Contentful(404)

object MethodNotAllowed extends Contentful(405)

object NotAcceptable extends Contentful(406)

object ProxyAuthenticationRequired extends Contentful(407)

object RequestTimeout extends Contentful(408)

object Conflict extends Contentful(409)

object Gone extends Contentful(410)

object LengthRequired extends Contentful(411)

object PreconditionFailed extends Contentful(412)

object RequestEntityTooLarge extends Contentful(413)

object RequestURITooLong extends Contentful(414)

object UnsupportedMediaType extends Contentful(415)

object RequestedRangeNotSatisfiable extends Contentful(416)

object ExpectationFailed extends Contentful(417)

object UnprocessableEntity extends Contentful(422)

object Locked extends Contentful(423)

object FailedDependency extends Contentful(424)

object UpgradeRequired extends Contentful(426)

object PreconditionRequired extends Contentful(428)

object TooManyRequests extends Contentful(429)

object RequestHeaderFieldsTooLarge extends Contentful(431)

object InternalServerError extends Contentful(500)

object NotImplemented extends Contentful(501)

object BadGateway extends Contentful(502)

object ServiceUnavailable extends Contentful(503)

object GatewayTimeout extends Contentful(504)

object HTTPVersionNotSupported extends Contentful(505)

object VariantAlsoNegotiates extends Contentful(506)

object InsufficientStorage extends Contentful(507)

object LoopDetected extends Contentful(508)

object NotExtended extends Contentful(510)

object NetworkAuthenticationRequired extends Contentful(511)

