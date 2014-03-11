package org.scalatra
package auth

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import util.RicherString._

trait ScentryConfig {
  val login = "/login"
  val returnTo = "/"
  val returnToKey = "returnTo"
  val failureUrl = "/unauthenticated"
}

trait ScentrySupport[UserType <: AnyRef] extends Initializable {
  self: ScalatraBase ⇒

  type ScentryConfiguration <: ScentryConfig

  protected def fromSession: PartialFunction[String, UserType]
  protected def toSession: PartialFunction[UserType, String]
  protected def scentryConfig: ScentryConfiguration

  private[this] var _strategiesFromConfig = List[String]()

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)
    readStrategiesFromConfig(config)
  }

  private def initializeScentry(request: HttpServletRequest) = {
    val store = new ScentryAuthStore.SessionAuthStore(this)
    request.setAttribute(Scentry.ScentryRequestKey, new Scentry[UserType](self, toSession, fromSession, store))
  }

  private def readStrategiesFromConfig(config: ConfigT) =
    _strategiesFromConfig = {
      config.context.getInitParameter("scentry.strategies").blankOption map (s ⇒ (s split ";").toList) getOrElse Nil
  }

  private def registerStrategiesFromConfig(req: HttpServletRequest, resp: HttpServletResponse) = _strategiesFromConfig foreach { strategyClassName ⇒
    val strategy = Class.forName(strategyClassName).newInstance.asInstanceOf[ScentryStrategy[UserType]]
    strategy registerWith scentry(req, resp)
  }

  private[this] def createScentry(req: HttpServletRequest, resp: HttpServletResponse) = {
    initializeScentry(req)
    configureScentry(req, resp)
    registerStrategiesFromConfig(req, resp)
    registerAuthStrategies(req, resp)
  }

  protected def configureScentry(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()

  /**
   * Override this method to register authentication strategies specific to this servlet.
   *     registerAuthStrategy('UserPassword, app => new UserPasswordStrategy(app))
   */
  protected def registerAuthStrategies(req: HttpServletRequest, resp: HttpServletResponse): Unit = ()

  protected def scentry(implicit req: HttpServletRequest, resp: HttpServletResponse): Scentry[UserType] = {
    if (!req.contains(Scentry.ScentryRequestKey))
      createScentry(req, resp)
    req(Scentry.ScentryRequestKey).asInstanceOf[Scentry[UserType]]
  }
  protected def scentryOption(implicit request: HttpServletRequest): Option[Scentry[UserType]] = Option(request(Scentry.ScentryRequestKey)).map(_.asInstanceOf[Scentry[UserType]])
  protected def userOption(implicit request: HttpServletRequest, resp: HttpServletResponse): Option[UserType] = scentry.userOption
  implicit protected def user(implicit request: HttpServletRequest, resp: HttpServletResponse): UserType = scentry.user
  protected def user_=(user: UserType)(implicit request: HttpServletRequest, resp: HttpServletResponse) = scentry.user = user
  protected def isAuthenticated(implicit request: HttpServletRequest, resp: HttpServletResponse): Boolean = scentry.isAuthenticated
  protected def isAnonymous(implicit request: HttpServletRequest, resp: HttpServletResponse): Boolean = !isAuthenticated

  protected def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) = scentry.authenticate()

  protected def logOut()(implicit request: HttpServletRequest, response: HttpServletResponse) = scentry.logout()

}
