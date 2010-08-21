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
  type ScentryConfiguration <: ScentryConfig

  protected def fromSession: PartialFunction[String, UserType]
  protected def toSession: PartialFunction[UserType, String]
  protected val scentryConfig: ScentryConfiguration

  private val _scentry = new DynamicVariable[Scentry[UserType]](null)

  abstract override def handle(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) = {
    val app = ScalatraKernelProxy(session, params, redirect _)
    _scentry.withValue(new Scentry[UserType](app, toSession, fromSession)) {
      registerStrategies
      super.handle(servletRequest, servletResponse)
    }
  }

  /**
   * Override this method to register authentication strategies specific to this servlet.
   *     scentry.registerStrategy('UserPassword, app => new UserPasswordStrategy(app))
   */
  protected def registerStrategies = {}
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