package org.scalatra


import java.security.SecureRandom

trait CSRFTokenSupport { self: ScalatraKernel =>

  private val CSRF_KEY = ScalatraKernel.csrfKey

  before {
    if (request.isWrite && session.get(CSRF_KEY) != params.get(CSRF_KEY))
      halt(403, "Request tampering detected!")
    prepareCSRFToken
  }

  protected def prepareCSRFToken = {
    session.getOrElseUpdate(CSRF_KEY, generateCSRFToken)
  }

  private def hexEncode(bytes: Array[Byte]) =  ((new StringBuilder(bytes.length * 2) /: bytes) { (sb, b) =>
    if((b.toInt & 0xff) < 0x10) sb.append("0")
    sb.append(Integer.toString(b.toInt & 0xff, 16))
  }).toString

  protected def generateCSRFToken = {
    val tokenVal = new Array[Byte](20)
    (new SecureRandom).nextBytes(tokenVal)
    hexEncode(tokenVal)
  }
}