package org.scalatra.auth

import collection.mutable.{ HashMap, Map => MMap }
import scala.PartialFunction

object Scentry {

  type StrategyFactory[UserType <: AnyRef] = ScalatraKernelProxy => ScentryStrategy[UserType]

  private val _globalStrategies = new HashMap[Symbol, StrategyFactory[_ <: AnyRef]]()

  def registerStrategy[UserType <: AnyRef](name: Symbol, strategyFactory: StrategyFactory[UserType]) =
    _globalStrategies += (name -> strategyFactory)

  def globalStrategies = _globalStrategies
  def clearGlobalStrategies = _globalStrategies.clear

  val scentryAuthKey = "scentry.auth.default.user"




}
class Scentry[UserType <: AnyRef](
        app: ScalatraKernelProxy,
        serialize: PartialFunction[UserType, String],
        deserialize: PartialFunction[String, UserType] ) {

  type StrategyType = ScentryStrategy[UserType]
  type StrategyFactory = ScalatraKernelProxy => StrategyType

  import Scentry._
  private val _strategies = new HashMap[Symbol, StrategyFactory]()
  private var _user: UserType = null.asInstanceOf[UserType]

  def session = app.session
  def params = app.params
  def redirect(uri: String) = app.redirect(uri)

  def registerStrategy(name: Symbol, strategyFactory: StrategyFactory) =
    _strategies += (name -> strategyFactory)

  def strategies: MMap[Symbol, ScentryStrategy[UserType]] =
    (globalStrategies ++ _strategies) map { case (nm, fact) => (nm -> fact.asInstanceOf[StrategyFactory](app)) }

  def user = if (_user != null) _user else { 
    val key = session.getAttribute(scentryAuthKey).asInstanceOf[String]
    if (key != null && key.trim.length > 0 ) {
      runCallbacks() { _.beforeFetch(key) }
      val res = fromSession(key)
      if (res != null) runCallbacks() { _.afterFetch(res) }
      _user = res
      res
    }
    else null
  }

  def user_=(v: UserType) = {
    _user = v
    if (v != null) {
      runCallbacks() { _.beforeSetUser(v) }
      val res = toSession(v)
      session.setAttribute(scentryAuthKey, res)
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
      runCallbacks(_.valid_?) { _.beforeAuthenticate }
      if(acc.isEmpty && strat.valid_? && (names.isEmpty || names.contains(nm))) {
        strat.authenticate_! match {
          case Some(usr)  => (nm, usr) :: acc
          case _ => acc
        }
       } else acc
    }.headOption foreach { case (stratName, usr) =>
      runCallbacks() { _.afterAuthenticate(stratName, usr) }
      user = usr
    }
  }

  def logout = {
    val usr = user.asInstanceOf[UserType]
    runCallbacks() { _.beforeLogout(usr) }
    if (_user != null) _user = null.asInstanceOf[UserType]
    session.invalidate
    runCallbacks() { _.afterLogout(usr) }
  }

  private def runCallbacks(guard: StrategyType => Boolean = s => true)(which: StrategyType => Unit) {
    strategies foreach { case (_, v) if guard(v) => which(v)}
  }
}