package org.scalatra


import java.security.SecureRandom

object GenerateId {
  def apply(): String = {
    generateCsrfToken()
  }

  private def hexEncode(bytes: Array[Byte]) =  ((new StringBuilder(bytes.length * 2) /: bytes) { (sb, b) =>
    if((b.toInt & 0xff) < 0x10) sb.append("0")
    sb.append(Integer.toString(b.toInt & 0xff, 16))
  }).toString

  protected def generateCsrfToken() = {
    val tokenVal = new Array[Byte](20)
    (new SecureRandom).nextBytes(tokenVal)
    hexEncode(tokenVal)
  }

  @deprecated("Use generateCsrfToken()")
  protected def generateCSRFToken() = generateCsrfToken()
}

object CsrfTokenSupport {
  val DefaultKey = "org.scalatra.CsrfTokenSupport.key".intern
}

/**
 * Provides cross-site request forgery protection.
 *
 * Adds a before filter.  If a request is determined to be forged, the
 * `handleForgery()` hook is invoked.  Otherwise, a token for the next
 * request is prepared with `prepareCsrfToken`.
 */
trait CsrfTokenSupport { self: ScalatraKernel =>
  /**
   * The key used to store the token on the session, as well as the parameter
   * of the request.
   */
  protected def csrfKey: String = CsrfTokenSupport.DefaultKey

  /**
   * Returns the token from the session.
   */
  protected def csrfToken: String = session(csrfKey).asInstanceOf[String]

  before() {
    if (isForged) {
      handleForgery()
    }
    prepareCsrfToken()
  }

  /**
   * Tests whether a request with a unsafe method is a potential cross-site
   * forgery.
   *
   * @return true if the request is an unsafe method (POST, PUT, DELETE, TRACE,
   * CONNECT, PATCH) and the request parameter at `csrfKey` does not match
   * the session key of the same name.
   */
  protected def isForged: Boolean = {
    request.isWrite && session.get(csrfKey) != params.get(csrfKey)
  }

  /**
   * Take an action when a forgery is detected. The default action
   * halts further request processing and returns a 403 HTTP status code.
   */
  protected def handleForgery() {
    halt(403, "Request tampering detected!")
  }

  /**
   * Prepares a CSRF token.  The default implementation uses `GenerateId`
   * and stores it on the session.
   */
  protected def prepareCsrfToken() = {
    session.getOrElseUpdate(csrfKey, GenerateId())
  }

  @deprecated("Use prepareCsrfToken()")
  protected def prepareCSRFToken() = prepareCsrfToken()
}

