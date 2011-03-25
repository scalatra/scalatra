package org.scalatra


import java.security.SecureRandom

object GenerateId {
  def apply(): String = {
    generateCSRFToken()
  }

  private def hexEncode(bytes: Array[Byte]) =  ((new StringBuilder(bytes.length * 2) /: bytes) { (sb, b) =>
    if((b.toInt & 0xff) < 0x10) sb.append("0")
    sb.append(Integer.toString(b.toInt & 0xff, 16))
  }).toString

  protected def generateCSRFToken() = {
    val tokenVal = new Array[Byte](20)
    (new SecureRandom).nextBytes(tokenVal)
    hexEncode(tokenVal)
  }
}

trait CSRFTokenSupport { self: ScalatraKernel =>

  protected def csrfKey = ScalatraKernel.csrfKey
  protected def csrfToken = session(csrfKey).asInstanceOf[String]

  before {
    if (isForged) {
      handleForgery()
    }
    prepareCSRFToken()
  }

  /**
   * Test whether a POST request is a potential cross-site forgery.
   *
   * @return Returns true if the POST request is suspect.
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

  protected def prepareCSRFToken() = {
    session.getOrElseUpdate(csrfKey, GenerateId())
  }

}
