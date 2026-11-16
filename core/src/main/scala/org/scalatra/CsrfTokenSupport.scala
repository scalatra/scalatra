package org.scalatra

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, SecureRandom}
import org.scalatra.ServletCompat.http.HttpServletRequest

object GenerateId {

  def apply(): String = generateCsrfToken()

  private[this] def hexEncode(bytes: Array[Byte]): String = {
    bytes
      .foldLeft(new StringBuilder(bytes.length * 2)) { (sb, b) =>
        if ((b.toInt & 0xff) < 0x10) sb.append("0")
        sb.append(Integer.toString(b.toInt & 0xff, 16))
      }
      .toString
  }

  protected def generateCsrfToken(): String = {
    val tokenVal = new Array[Byte](20)
    (new SecureRandom).nextBytes(tokenVal)
    hexEncode(tokenVal)
  }

}

object CsrfTokenSupport {

  val DefaultKey = "org.scalatra.CsrfTokenSupport.key"

  val HeaderNames = Vector("X-CSRF-TOKEN")

  /** Request headers with credentials that a browser sends by itself, also with a cross-site request. An unsafe request
    * with one of them must carry the token.
    */
  val CredentialHeaders = Vector("Cookie")

  /** Removes the token from the request's session, if there is one; the next read creates a new token. For code that
    * doesn't mix in [[CsrfTokenSupport]], e.g. a sign-in servlet next to a filter with the check.
    */
  def resetToken(request: HttpServletRequest, key: String = DefaultKey): Unit =
    Option(request.getSession(false)).foreach(_.removeAttribute(key))

  private[scalatra] def tokenMatches(expected: String, actual: String): Boolean =
    actual.nonEmpty &&
      MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))

}

/** Provides cross-site request forgery protection.
  *
  * The token is stored in the session. It is created when it is first read with `csrfToken`, e.g. when a page renders a
  * form, so requests that don't use it get no session. Override `csrfEagerToken` to create it before every request.
  *
  * Adds a before filter. An unsafe request that carries credentials a browser sends by itself (a `Cookie` header, see
  * `csrfCredentialHeaders`) must send the session's token in the request parameter `csrfKey` or in one of the
  * `csrfHeaderNames` headers; otherwise the `handleForgery()` hook is invoked. Requests without such credentials, e.g.
  * from API clients, are not checked.
  *
  * Call `resetCsrfToken()` when a user signs in, so that a token known before the sign-in is not valid after it.
  */
trait CsrfTokenSupport { this: ScalatraBase =>

  before(isForged) { handleForgery() }
  before(csrfEagerToken) { prepareCsrfToken() }

  /** Tests whether a request with an unsafe method is a potential cross-site forgery. Creates no session.
    *
    * @return
    *   true if the request is an unsafe method (POST, PUT, DELETE, TRACE, CONNECT, PATCH), has one of the
    *   `csrfCredentialHeaders`, and neither the request parameter at `csrfKey` nor one of the `csrfHeaderNames` headers
    *   matches the session's token. A request without a session or without a token in the session matches nothing.
    */
  protected def isForged: Boolean =
    !request.requestMethod.isSafe &&
      csrfCredentialHeaders.exists(request.headers.get(_).isDefined) &&
      !existingCsrfToken.exists { token =>
        (params.get(csrfKey) ++ csrfHeaderNames.flatMap(request.headers.get))
          .exists(CsrfTokenSupport.tokenMatches(token, _))
      }

  /** Take an action when a forgery is detected. The default action halts further request processing and returns a 403
    * HTTP status code.
    */
  protected def handleForgery(): Unit = {
    halt(403, "Request tampering detected!")
  }

  /** Prepares a CSRF token: returns the session's token, or creates a token with `GenerateId` and stores it on the
    * session, creating the session if needed. Called by `csrfToken` if the session has no token, and before every
    * request if `csrfEagerToken` is true.
    */
  protected def prepareCsrfToken(): String = {
    session.getOrElseUpdate(csrfKey, GenerateId()).toString
  }

  /** The key used to store the token on the session, as well as the parameter of the request.
    */
  def csrfKey: String = CsrfTokenSupport.DefaultKey

  /** Whether the token is prepared before every request, which creates a session for every request (the behaviour of
    * earlier versions). By default the token is created when `csrfToken` is first read.
    */
  protected def csrfEagerToken: Boolean = false

  /** The request headers that make an unsafe request subject to the check. Add `Authorization` if browsers send Basic
    * authentication to the application.
    */
  protected def csrfCredentialHeaders: Seq[String] = CsrfTokenSupport.CredentialHeaders

  /** The request headers that may carry the token.
    */
  protected def csrfHeaderNames: Seq[String] = CsrfTokenSupport.HeaderNames

  /** Returns the token from the session. Creates the token, and the session, if needed (see `prepareCsrfToken`).
    */
  def csrfToken(implicit request: HttpServletRequest): String =
    existingCsrfToken(request).getOrElse(prepareCsrfToken())

  /** Removes the token from the session; the next `csrfToken` creates a new one. Call it when a user signs in.
    */
  def resetCsrfToken()(implicit request: HttpServletRequest): Unit =
    CsrfTokenSupport.resetToken(request, csrfKey)

  private def existingCsrfToken(implicit request: HttpServletRequest): Option[String] =
    sessionOption.flatMap(session => Option(session.getAttribute(csrfKey))).map(_.toString)

}

trait XsrfTokenSupport { this: ScalatraBase =>

  import org.scalatra.XsrfTokenSupport.*

  /** The key used to store the token on the session, as well as the parameter of the request.
    */
  def xsrfKey: String = DefaultKey

  /** Returns the token from the session.
    */
  def xsrfToken(implicit request: HttpServletRequest): String =
    request.getSession.getAttribute(xsrfKey).asInstanceOf[String]

  def xsrfGuard(only: RouteTransformer*): Unit = {
    before(((only.toSeq ++ Seq[RouteTransformer](isForged)))*) { handleForgery() }
  }

  before() { prepareXsrfToken() }

  /** Tests whether a request with a unsafe method is a potential cross-site forgery.
    *
    * @return
    *   true if the request is an unsafe method (POST, PUT, DELETE, TRACE, CONNECT, PATCH) and the request parameter at
    *   `xsrfKey` does not match the session key of the same name.
    */
  protected def isForged: Boolean =
    !request.requestMethod.isSafe &&
      session.get(xsrfKey) != params.get(xsrfKey) &&
      !HeaderNames.map(request.headers.get).contains(session.get(xsrfKey))

  /** Take an action when a forgery is detected. The default action halts further request processing and returns a 403
    * HTTP status code.
    */
  protected def handleForgery(): Unit = {
    halt(403, "Request tampering detected!")
  }

  /** Prepares a XSRF token. The default implementation uses `GenerateId` and stores it on the session.
    */
  protected def prepareXsrfToken(): Unit = {
    session.getOrElseUpdate(xsrfKey, GenerateId())
    val cookieOpt = cookies.get(CookieKey)
    if (cookieOpt.isEmpty || cookieOpt != session.get(xsrfKey)) {
      cookies += CookieKey -> xsrfToken
    }
  }
}

object XsrfTokenSupport {

  val DefaultKey = "org.scalatra.XsrfTokenSupport.key"

  val HeaderNames = Vector("X-XSRF-TOKEN")

  val CookieKey = "XSRF-TOKEN"

}
