package org.scalatra
package auth

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.util.RicherString._

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

  private def initializeScentry = {
    val store = new ScentryAuthStore.SessionAuthStore(this)
    request.setAttribute(Scentry.ScentryRequestKey, new Scentry[UserType](self, toSession, fromSession, store))
  }

  private def readStrategiesFromConfig(config: ConfigT) =
    _strategiesFromConfig = {
      config.context.getInitParameter("scentry.strategies").blankOption map (s ⇒ (s split ";").toList) getOrElse Nil
    }

  private def registerStrategiesFromConfig = _strategiesFromConfig foreach { strategyClassName ⇒
    val strategy = Class.forName(strategyClassName).newInstance.asInstanceOf[ScentryStrategy[UserType]]
    strategy registerWith scentry
  }

  private[this] def createScentry() = {
    initializeScentry
    configureScentry
    registerStrategiesFromConfig
    registerAuthStrategies
  }

  protected def configureScentry() = {

  }

  /**
   * Override this method to register authentication strategies specific to this servlet.
   *     registerAuthStrategy('UserPassword, app => new UserPasswordStrategy(app))
   */
  protected def registerAuthStrategies() = {

  }

  protected def scentry(implicit request: HttpServletRequest): Scentry[UserType] = {
    if (!request.contains(Scentry.ScentryRequestKey))
      createScentry()
    request(Scentry.ScentryRequestKey).asInstanceOf[Scentry[UserType]]
  }
  protected def scentryOption(implicit request: HttpServletRequest): Option[Scentry[UserType]] = Option(request(Scentry.ScentryRequestKey)).map(_.asInstanceOf[Scentry[UserType]])
  protected def userOption(implicit request: HttpServletRequest): Option[UserType] = scentry.userOption
  implicit protected def user(implicit request: HttpServletRequest): UserType = scentry.user
  protected def user_=(user: UserType)(implicit request: HttpServletRequest) = scentry.user = user
  protected def isAuthenticated(implicit request: HttpServletRequest): Boolean = scentry.isAuthenticated
  protected def isAnonymous(implicit request: HttpServletRequest): Boolean = !isAuthenticated

  protected def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) = scentry.authenticate()

  protected def logOut()(implicit request: HttpServletRequest, response: HttpServletResponse) = scentry.logout()

}
