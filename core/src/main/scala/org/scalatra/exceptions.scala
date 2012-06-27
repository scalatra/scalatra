package org.scalatra

class ScalatraException(message: String) extends Exception(message)

object SessionsDisableException {
  def apply() = throw new SessionsDisabledException
}
class SessionsDisabledException() extends ScalatraException("Sessions have been disabled for this app.")
