package org.scalatra
package auth

import collection.mutable.{ HashMap, Map => MMap }
import scala.PartialFunction
import ScentryAuthStore.{SessionAuthStore, ScentryAuthStore}
import util.RicherString

object Scentry {

  type StrategyFactory[UserType <: AnyRef] = ScalatraKernel => ScentryStrategy[UserType]

  private val _globalStrategies = new HashMap[Symbol, StrategyFactory[_ <: AnyRef]]()

  def registerStrategy[UserType <: AnyRef](name: Symbol, strategyFactory: StrategyFactory[UserType]) =
    _globalStrategies += (name -> strategyFactory)

  def globalStrategies = _globalStrategies
  def clearGlobalStrategies = _globalStrategies.clear

  val scentryAuthKey = "scentry.auth.default.user"
}

class Scentry[UserType <: AnyRef](
        app: ScalatraKernel,
        serialize: PartialFunction[UserType, String],
        deserialize: PartialFunction[String, UserType] ) {

  import RicherString._

  type StrategyType = ScentryStrategy[UserType]
  type StrategyFactory = ScalatraKernel => StrategyType

  import Scentry._
  private val _strategies = new HashMap[Symbol, StrategyFactory]()
  private var _user: UserType = null.asInstanceOf[UserType]
  private var _store: ScentryAuthStore = new SessionAuthStore(app.session)

  def setStore(newStore: ScentryAuthStore) = _store = newStore
  def store = _store
  def proxy = app

  def isAuthenticated = {
    userOption.isDefined
  }
  @deprecated("use isAuthenticated")
  def authenticated_? = isAuthenticated

  //def session = app.session
  def params = app.params
  def redirect(uri: String) = app.redirect(uri)

  def registerStrategy(name: Symbol, strategyFactory: StrategyFactory) =
    _strategies += (name -> strategyFactory)

  def strategies: MMap[Symbol, ScentryStrategy[UserType]] =
    (globalStrategies ++ _strategies) map { case (nm, fact) => (nm -> fact.asInstanceOf[StrategyFactory](app)) }

  def userOption: Option[UserType] = Option(user)

  def user : UserType = if (_user != null) _user else {
    val key = store.get
    if (key.isNonBlank) {
      runCallbacks() { _.beforeFetch(key) }
      val res = fromSession(key)
      if (res != null) runCallbacks() { _.afterFetch(res) }
      _user = res
      res
    }
    else null.asInstanceOf[UserType]
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
    case _ => throw new RuntimeException("You need to provide a session serializer for Scentry")
  }

  private def missingDeserializer: PartialFunction[String, UserType] = {
    case _ => throw new RuntimeException("You need to provide a session deserializer for Scentry")
  }

  def authenticate(names: Symbol*): Unit = {
    (List[(Symbol, UserType)]() /: strategies) { (acc, stratKv) =>
      val (nm, strat) = stratKv
      runCallbacks(_.isValid) { _.beforeAuthenticate }
      if(acc.isEmpty && strat.isValid && (names.isEmpty || names.contains(nm))) {
        strat.authenticate() match {
          case Some(usr)  => (nm, usr) :: acc
          case _ => acc
        }
       } else acc
    }.headOption foreach { case (stratName, usr) =>
      runCallbacks() { _.afterAuthenticate(stratName, usr) }
      user = usr
    }
  }

  def logout() = {
    val usr = user.asInstanceOf[UserType]
    runCallbacks() { _.beforeLogout(usr) }
    if (_user != null) _user = null.asInstanceOf[UserType]
    store.invalidate
    runCallbacks() { _.afterLogout(usr) }
  }

  private def runCallbacks(guard: StrategyType => Boolean = s => true)(which: StrategyType => Unit) {
    strategies foreach {
      case (_, v) if guard(v) => which(v)
      case _ => // guard failed
    }
  }
}