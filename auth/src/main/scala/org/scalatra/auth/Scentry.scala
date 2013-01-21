package org.scalatra
package auth

import ScentryAuthStore.ScentryAuthStore
import collection.mutable
import grizzled.slf4j.Logger
import org.scalatra.util.RicherString._
import util.{MultiMapHeadView}

object Scentry {

  type StrategyFactory[UserType <: AnyRef] = ScalatraSyntax ⇒ ScentryStrategy[UserType]

  private val _globalStrategies = new mutable.HashMap[String, StrategyFactory[_ <: AnyRef]]()

  @deprecated("Use method `register` with strings instead.", "2.0")
  def registerStrategy[UserType <: AnyRef](name: Symbol, strategyFactory: StrategyFactory[UserType]) {
    _globalStrategies += (name.name -> strategyFactory)
  }

  def register[UserType <: AnyRef](name: String, strategyFactory: StrategyFactory[UserType]) {
    _globalStrategies += (name -> strategyFactory)
  }

  def globalStrategies = _globalStrategies
  def clearGlobalStrategies() { _globalStrategies.clear() }

  val scentryAuthKey = "scentry.auth.default.user"
  val ScentryRequestKey = "org.scalatra.auth.Scentry"
}

class Scentry[UserType <: AnyRef](
    app: ScalatraSyntax,
    serialize: PartialFunction[UserType, String],
    deserialize: PartialFunction[String, UserType],
    private[this] var _store: ScentryAuthStore) {

  private[this] lazy val logger = Logger(getClass)
  type StrategyType = ScentryStrategy[UserType]
  type StrategyFactory = ScalatraSyntax ⇒ StrategyType

  import Scentry._

  private[this] val _strategies = new mutable.HashMap[String, StrategyFactory]()
  private[this] var _user: UserType = null.asInstanceOf[UserType]

  def store = _store
  def store_=(newStore: ScentryAuthStore) {
    _store = newStore
  }

  def isAuthenticated = {
    userOption.isDefined
  }

  //def session = app.session
  def params: Params = app.params
  def redirect(uri: String) { app.redirect(uri) }

  def register(strategy: => ScentryStrategy[UserType]) {
    register(strategy.name, ((_: ScalatraSyntax) => strategy))
  }
  
  def register(name: String, strategyFactory: StrategyFactory) {
    _strategies += (name -> strategyFactory)
  }

  def strategies: mutable.Map[String, ScentryStrategy[UserType]] =
    (globalStrategies ++ _strategies) map { case (nm, fact) ⇒ (nm -> fact.asInstanceOf[StrategyFactory](app)) }

  def userOption: Option[UserType] = Option(user)

  def user: UserType = if (_user != null) _user else {
    val key = store.get
    if (key.nonBlank) {
      runCallbacks() { _.beforeFetch(key) }
      val res = fromSession(key)
      if (res != null) runCallbacks() { _.afterFetch(res) }
      _user = res
      res
    } else null.asInstanceOf[UserType]
  }

  def user_=(v: UserType) = {
    _user = v
    if (v != null) {
      runCallbacks() { _.beforeSetUser(v) }
      val res = toSession(v)
      store.set(res)
      runCallbacks() { _.afterSetUser(v) }
      res
    } else v
  }

  def fromSession = deserialize orElse missingDeserializer

  def toSession = serialize orElse missingSerializer

  private def missingSerializer: PartialFunction[UserType, String] = {
    case _ ⇒ throw new RuntimeException("You need to provide a session serializer for Scentry")
  }

  private def missingDeserializer: PartialFunction[String, UserType] = {
    case _ ⇒ throw new RuntimeException("You need to provide a session deserializer for Scentry")
  }

//  @deprecated("Use the version that uses string keys instead", "2.0")
  def authenticate(name: Symbol, names: Symbol*): Option[UserType] =
    authenticate((Seq(name) ++ names.toSeq).map(_.name):_*)

  def authenticate(names: String*): Option[UserType] = {
    val r = runAuthentication(names: _*) map {
      case (stratName, usr) ⇒
        runCallbacks() { _.afterAuthenticate(stratName, usr) }
        user = usr
        user
    }
    if (names.isEmpty) r orElse { defaultUnauthenticated foreach (_.apply()); None }
    else r
  }

  private def runAuthentication(names: String*) = {
    val subset = if (names.isEmpty) strategies.values else strategies.filterKeys(names.contains).values
    (subset filter (_.isValid) map { strat =>
      logger.debug("Authenticating with: %s" format strat.name)
      runCallbacks(_.isValid) { _.beforeAuthenticate }
      strat.authenticate() match {
        case Some(usr) ⇒ Some(strat.name -> usr)
        case _         ⇒
          strat.unauthenticated()
          None
      }
    }).find(_.isDefined) getOrElse None
  }

  private def runUnauthenticated(names: String*) = {
    (strategies filter { case (name, strat) ⇒ strat.isValid && (names.isEmpty || names.contains(name)) }).values.toList match {
      case Nil ⇒ {
        defaultUnauthenticated foreach { _.apply() }
      }
      case l ⇒ {
        l foreach { s ⇒ runCallbacks(_.name == s.name) { _.unauthenticated() } }
      }
    }
    None

  }

  private[this] var defaultUnauthenticated: Option[() ⇒ Unit] = None

  def unauthenticated(callback: ⇒ Unit) {
    defaultUnauthenticated = Some(() ⇒ callback)
  }

  def logout() {
    val usr = user
    runCallbacks() { _.beforeLogout(usr) }
    if (_user != null) _user = null.asInstanceOf[UserType]
    store.invalidate()
    runCallbacks() { _.afterLogout(usr) }
  }

  private def runCallbacks(guard: StrategyType ⇒ Boolean = s ⇒ true)(which: StrategyType ⇒ Unit) {
    strategies foreach {
      case (_, v) if guard(v) ⇒ which(v)
      case _                  ⇒ // guard failed
    }
  }
}

