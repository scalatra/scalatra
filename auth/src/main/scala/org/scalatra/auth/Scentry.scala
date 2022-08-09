package org.scalatra
package auth

import jakarta.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.auth.ScentryAuthStore.ScentryAuthStore
import org.scalatra.servlet.ServletApiImplicits._
import org.scalatra.util.RicherString._
import org.slf4j.LoggerFactory

import scala.collection.mutable

object Scentry {

  type StrategyFactory[UserType <: AnyRef] = ScalatraBase => ScentryStrategy[UserType]

  private val _globalStrategies = new mutable.HashMap[String, StrategyFactory[_ <: AnyRef]]()

  def register[UserType <: AnyRef](name: String, strategyFactory: StrategyFactory[UserType]): Unit = {
    _globalStrategies += (name -> strategyFactory)
  }

  def globalStrategies = _globalStrategies
  def clearGlobalStrategies(): Unit = { _globalStrategies.clear() }

  val scentryAuthKey = "scentry.auth.default.user"
  val ScentryRequestKey = "org.scalatra.auth.Scentry"
}

class Scentry[UserType <: AnyRef](
  app: ScalatraBase,
  serialize: PartialFunction[UserType, String],
  deserialize: PartialFunction[String, UserType],
  private[this] var _store: ScentryAuthStore) {

  private[this] lazy val logger = LoggerFactory.getLogger(getClass)
  type StrategyType = ScentryStrategy[UserType]
  type StrategyFactory = ScalatraBase => StrategyType

  import org.scalatra.auth.Scentry._

  private[this] val _strategies = new mutable.HashMap[String, StrategyFactory]()
  private[this] def _user(implicit request: HttpServletRequest): UserType =
    request.get(scentryAuthKey).orNull.asInstanceOf[UserType]

  def store = _store
  def store_=(newStore: ScentryAuthStore): Unit = {
    _store = newStore
  }

  def isAuthenticated(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    userOption.isDefined
  }

  //def session = app.session
  def params(implicit request: HttpServletRequest): Params = app.params(request)
  def redirect(uri: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
    app.redirect(uri)(request, response)
  }

  def register(strategy: => ScentryStrategy[UserType]): Unit = {
    register(strategy.name, ((_: ScalatraBase) => strategy))
  }

  def register(name: String, strategyFactory: StrategyFactory): Unit = {
    _strategies += (name -> strategyFactory)
  }

  def strategies: mutable.Map[String, ScentryStrategy[UserType]] =
    (globalStrategies ++ _strategies) map { case (nm, fact) => (nm -> fact.asInstanceOf[StrategyFactory](app)) }

  def userOption(implicit request: HttpServletRequest, response: HttpServletResponse): Option[UserType] =
    Option(_user) orElse {
      store.get.blankOption flatMap { key =>
        runCallbacks() { _.beforeFetch(key) }
        val o = fromSession lift key flatMap (Option(_)) map { res =>
          runCallbacks() { _.afterFetch(res) }
          request(scentryAuthKey) = res
          res
        }
        if (o.isEmpty) request(scentryAuthKey) = null
        o
      }
    }

  def user(implicit request: HttpServletRequest, response: HttpServletResponse): UserType =
    userOption getOrElse null.asInstanceOf[UserType]

  def user_=(v: UserType)(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    request(scentryAuthKey) = v
    if (v != null) {
      runCallbacks() { _.beforeSetUser(v) }
      val res = toSession(v)
      store.set(res)
      runCallbacks() { _.afterSetUser(v) }
      res
    } else ""
  }

  def fromSession: PartialFunction[String, UserType] = deserialize orElse missingDeserializer

  def toSession: PartialFunction[UserType, String] = serialize orElse missingSerializer

  private def missingSerializer: PartialFunction[UserType, String] = {
    case _ => throw new RuntimeException("You need to provide a session serializer for Scentry")
  }

  private def missingDeserializer: PartialFunction[String, UserType] = {
    case _ => throw new RuntimeException("You need to provide a session deserializer for Scentry")
  }

  def authenticate(names: String*)(implicit request: HttpServletRequest, response: HttpServletResponse): Option[UserType] = {
    val r = runAuthentication(names: _*) map {
      case (stratName, usr) =>
        runCallbacks() { _.afterAuthenticate(stratName, usr) }
        user_=(usr)
        user
    }
    if (names.isEmpty) r orElse { defaultUnauthenticated foreach (_.apply()); None }
    else r
  }

  private[this] def runAuthentication(names: String*)(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    val subset = if (names.isEmpty) strategies.values else strategies.filterKeys(names.contains).values
    subset.withFilter(_.isValid).flatMap { strat =>
      logger.debug("Authenticating with: %s" format strat.name)
      runCallbacks(_.isValid) { _.beforeAuthenticate }
      strat.authenticate() match {
        case Some(usr) => Some(strat.name -> usr)
        case _ =>
          strat.unauthenticated()
          None
      }
    }.headOption
  }

  private[this] var defaultUnauthenticated: Option[() => Unit] = None

  def unauthenticated(callback: => Unit): Unit = {
    defaultUnauthenticated = Some(() => callback)
  }

  def logout()(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
    val usr = user
    runCallbacks() { _.beforeLogout(usr) }
    request -= scentryAuthKey
    store.invalidate()
    runCallbacks() { _.afterLogout(usr) }
  }

  private[this] def runCallbacks(guard: StrategyType => Boolean = s => true)(which: StrategyType => Unit): Unit = {
    strategies foreach {
      case (_, v) if guard(v) => which(v)
      case _ => // guard failed
    }
  }
}

