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

trait ScentrySupport[TypeForUser <: AnyRef] extends Handler with Initializable with CookieSupport {
  self: ScalatraBase ⇒

  type UserType = TypeForUser
  type ScentryConfiguration <: ScentryConfig

  protected def fromSession: PartialFunction[String, UserType]
  protected def toSession: PartialFunction[UserType, String]
  protected def scentryConfig: ScentryConfiguration

  private var _strategiesFromConfig = List[String]()

  abstract override def initialize(config: ConfigT) {
    super.initialize(config)
    readStrategiesFromConfig(config)
  }

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) = {
    withRequest(req) {
      initializeScentry
      configureScentry
      registerStrategiesFromConfig
      registerAuthStrategies
      super.handle(req, res)
    }
  }

  private def initializeScentry = {
    val store = self match {
      case a: SessionSupport => new ScentryAuthStore.SessionAuthStore(a.session)
      case a: ScalatraBase with CookieSupport => new ScentryAuthStore.CookieAuthStore(a)
      case _ => throw new ScalatraException("Scentry needs either SessionSupport or CookieSupport mixed in.")
    }
    request(Scentry.ScentryRequestKey) = new Scentry[UserType](self, toSession, fromSession, store)
  }

  private def readStrategiesFromConfig(config: Config) =
    _strategiesFromConfig = {
      config.context.getInitParameter("scentry.strategies").blankOption map (s ⇒ (s split ";").toList) getOrElse Nil
  }

  private def registerStrategiesFromConfig = _strategiesFromConfig foreach { strategyClassName ⇒
    val strategy = Class.forName(strategyClassName).newInstance.asInstanceOf[ScentryStrategy[UserType]]
    strategy registerWith scentry
  }

  protected def configureScentry = {

  }

  /**
   * Override this method to register authentication strategies specific to this servlet.
   *     registerAuthStrategy('UserPassword, app => new UserPasswordStrategy(app))
   */
  protected def registerAuthStrategies = {

  }

  protected def scentry: Scentry[UserType] = request(Scentry.ScentryRequestKey).asInstanceOf[Scentry[UserType]]
  protected def scentryOption: Option[Scentry[UserType]] = Option(request(Scentry.ScentryRequestKey)).map(_.asInstanceOf[Scentry[UserType]])
  protected def userOption: Option[UserType] = scentry.userOption
  implicit protected def user: UserType = scentry.user
  protected def user_=(user: UserType) = scentry.user = user
  protected def isAuthenticated: Boolean = scentry.isAuthenticated
  protected def isAnonymous: Boolean = !isAuthenticated
  @deprecated("use isAuthenticated", "2.0.0")
  protected def authenticated_? : Boolean = isAuthenticated
  @deprecated("use isAnonymous", "2.0.0")
  protected def unAuthenticated_? : Boolean = !isAuthenticated

  protected def authenticate() = {
    scentry.authenticate()
  }

  protected def logOut() = scentry.logout()

  @deprecated("use logOut()", "2.0.0")
  protected def logOut_! = logOut()
}
