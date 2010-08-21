package org.scalatra.auth

import org.scalatra.{Handler, ScalatraKernel}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import util.DynamicVariable


trait ScentryConfig {
  val login = "/login"
  val returnTo = "/"
  val returnToKey = "returnTo"
  val failureUrl = "/unauthenticated"
}

trait ScentrySupport[TypeForUser <: AnyRef] extends Handler {
  self : ScalatraKernel =>

  type UserType = TypeForUser
  protected def fromSession: PartialFunction[String, UserType]
  protected def toSession: PartialFunction[UserType, String]
  type ScentryConfiguration <: ScentryConfig
  protected val scentryConfig: ScentryConfiguration

  private val _scentry = new DynamicVariable[Scentry[UserType]](null)


  abstract override def handle(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) = {
    val ctxt = AuthenticationContext(session, params, redirect _)
    _scentry.withValue(new Scentry[UserType](ctxt, toSession, fromSession)) {
      super.handle(servletRequest, servletResponse)
    }
  }

  protected def scentry = _scentry.value
  protected def scentryOption = Option(scentry)
  protected def user = scentry.user
  protected def authenticated_? = session(Scentry.scentryAuthKey).isDefined
  protected def unAuthenticated_? = !authenticated_?

  protected def authenticate() = {
    scentry.authenticate()
  }

  protected def logOut_! = scentry.logout


}