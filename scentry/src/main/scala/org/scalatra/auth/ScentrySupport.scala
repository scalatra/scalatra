package org.scalatra.auth

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import util.DynamicVariable
import org.scalatra.{Initializable, Handler, ScalatraKernel}
import javax.servlet.{FilterConfig, ServletConfig}

trait ScentryConfig {
  val login = "/login"
  val returnTo = "/"
  val returnToKey = "returnTo"
  val failureUrl = "/unauthenticated"
}

trait ScentrySupport[TypeForUser <: AnyRef] extends Handler with Initializable {
  self : ScalatraKernel =>

  type UserType = TypeForUser
  type ScentryConfiguration <: ScentryConfig

  protected def fromSession: PartialFunction[String, UserType]
  protected def toSession: PartialFunction[UserType, String]
  protected val scentryConfig: ScentryConfiguration

  private val _scentry = new DynamicVariable[Scentry[UserType]](null)
  private var _strategiesFromConfig = List[String]()

  abstract override def initialize(config: Config) {
    super.initialize(config)
    readStrategiesFromConfig(config)
  }

  abstract override def handle(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) = {
    val app = ScalatraKernelProxy(session, params, redirect _, request, response)
    _scentry.withValue(new Scentry[UserType](app, toSession, fromSession)) {
      registerStrategiesFromConfig
      registerAuthStrategies
      super.handle(servletRequest, servletResponse)
    }
  }

  private def readStrategiesFromConfig(config: Config) = _strategiesFromConfig = ((config match {
    case servletConfig: ServletConfig =>
      servletConfig.getInitParameter("scentry.strategies")
    case filterConfig: FilterConfig =>
      filterConfig.getInitParameter("scentry.strategies")
    case _ => ""
  }) split ";").toList

  private def registerStrategiesFromConfig = _strategiesFromConfig foreach { strategyClassName =>
    val strategy = Class.forName(strategyClassName).newInstance.asInstanceOf[ScentryStrategy[UserType]]
    strategy registerWith scentry
  }


  /**
   * Override this method to register authentication strategies specific to this servlet.
   *     registerAuthStrategy('UserPassword, app => new UserPasswordStrategy(app))
   */
  protected def registerAuthStrategies = {

  }

  protected def scentry = _scentry.value
  protected def scentryOption = Option(scentry)
  protected def user = scentry.user
  protected def user_=(user: UserType) = scentry.user = user
  protected def authenticated_? = session(Scentry.scentryAuthKey).isDefined
  protected def unAuthenticated_? = !authenticated_?

  protected def authenticate() = {
    scentry.authenticate()
  }

  protected def logOut_! = scentry.logout


}