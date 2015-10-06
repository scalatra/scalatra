package org.scalatra

import java.security.SecureRandom
import javax.servlet.http.HttpServletRequest

object GenerateId {

  def apply(): String = generateCsrfToken()

  private[this] def hexEncode(bytes: Array[Byte]): String = {
    ((new StringBuilder(bytes.length * 2) /: bytes) { (sb, b) =>
      if ((b.toInt & 0xff) < 0x10) sb.append("0")
      sb.append(Integer.toString(b.toInt & 0xff, 16))
    }).toString
  }

  protected def generateCsrfToken(): String = {
    val tokenVal = new Array[Byte](20)
    (new SecureRandom).nextBytes(tokenVal)
    hexEncode(tokenVal)
  }

  @deprecated("Use generateCsrfToken()", "2.0.0")
  protected def generateCSRFToken(): String = generateCsrfToken()

}

object CsrfTokenSupport {

  val DefaultKey = "org.scalatra.CsrfTokenSupport.key"

  val HeaderNames = Vector("X-CSRF-TOKEN")

}

/**
 * Provides cross-site request forgery protection.
 *
 * Adds a before filter.  If a request is determined to be forged, the
 * `handleForgery()` hook is invoked.  Otherwise, a token for the next
 * request is prepared with `prepareCsrfToken`.
 */
trait CsrfTokenSupport { this: ScalatraBase =>

  before(isForged) { handleForgery() }
  before() { prepareCsrfToken() }

  /**
   * Tests whether a request with a unsafe method is a potential cross-site
   * forgery.
   *
   * @return true if the request is an unsafe method (POST, PUT, DELETE, TRACE,
   * CONNECT, PATCH) and the request parameter at `csrfKey` does not match
   * the session key of the same name.
   */
  protected def isForged: Boolean =
    !request.requestMethod.isSafe &&
      session.get(csrfKey) != params.get(csrfKey) &&
      !CsrfTokenSupport.HeaderNames.map(request.headers.get).contains(session.get(csrfKey))

  /**
   * Take an action when a forgery is detected. The default action
   * halts further request processing and returns a 403 HTTP status code.
   */
  protected def handleForgery(): Unit = {
    halt(403, "Request tampering detected!")
  }

  /**
   * Prepares a CSRF token.  The default implementation uses `GenerateId`
   * and stores it on the session.
   */
  protected def prepareCsrfToken(): String = {
    session.getOrElseUpdate(csrfKey, GenerateId()).toString
  }

  @deprecated("Use prepareCsrfToken()", "2.0.0")
  protected def prepareCSRFToken(): String = prepareCsrfToken()

  /**
   * The key used to store the token on the session, as well as the parameter
   * of the request.
   */
  def csrfKey: String = CsrfTokenSupport.DefaultKey

  /**
   * Returns the token from the session.
   */
  protected[scalatra] def csrfToken(implicit request: HttpServletRequest): String =
    request.getSession.getAttribute(csrfKey).asInstanceOf[String]

}

trait XsrfTokenSupport { this: ScalatraBase =>

  import org.scalatra.XsrfTokenSupport._
  /**
   * The key used to store the token on the session, as well as the parameter
   * of the request.
   */
  def xsrfKey: String = DefaultKey

  /**
   * Returns the token from the session.
   */
  def xsrfToken(implicit request: HttpServletRequest): String =
    request.getSession.getAttribute(xsrfKey).asInstanceOf[String]

  def xsrfGuard(only: RouteTransformer*): Unit = {
    before((only.toSeq ++ Seq[RouteTransformer](isForged)): _*) { handleForgery() }
  }

  before() { prepareXsrfToken() }

  /**
   * Tests whether a request with a unsafe method is a potential cross-site
   * forgery.
   *
   * @return true if the request is an unsafe method (POST, PUT, DELETE, TRACE,
   * CONNECT, PATCH) and the request parameter at `xsrfKey` does not match
   * the session key of the same name.
   */
  protected def isForged: Boolean =
    !request.requestMethod.isSafe &&
      session.get(xsrfKey) != params.get(xsrfKey) &&
      !HeaderNames.map(request.headers.get).contains(session.get(xsrfKey))

  /**
   * Take an action when a forgery is detected. The default action
   * halts further request processing and returns a 403 HTTP status code.
   */
  protected def handleForgery(): Unit = {
    halt(403, "Request tampering detected!")
  }

  /**
   * Prepares a XSRF token.  The default implementation uses `GenerateId`
   * and stores it on the session.
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

