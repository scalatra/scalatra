package org.scalatra.auth

import collection.mutable.{ HashMap, Map => MMap }
import scala.PartialFunction

object Scentry {

  type StrategyFactory[UserType <: AnyRef] = Scentry[UserType] => ScentryStrategy[UserType]

  private val _globalStrategies = new HashMap[Symbol, StrategyFactory[_ <: AnyRef]]()

  def registerStrategy[UserType <: AnyRef](name: Symbol, strategyFactory: StrategyFactory[UserType]) =
    _globalStrategies += (name -> strategyFactory)

  def globalStrategies = _globalStrategies
  def clearGlobalStrategies = _globalStrategies.clear

  val scentryAuthKey = "scentry.auth.default.user"




}
class Scentry[UserType <: AnyRef](
        app: AuthenticationContext,
        serialize: PartialFunction[UserType, String],
        deserialize: PartialFunction[String, UserType] ) {

  type StrategyType <: ScentryStrategy[UserType]
  type StrategyFactory = Scentry[UserType] => ScentryStrategy[UserType]

  import Scentry._
  private val _strategies = new HashMap[Symbol, StrategyFactory]()

  def session = app.session
  def params = app.params
  def redirect(uri: String) = app.redirect(uri)

  def registerStrategy(name: Symbol, strategyFactory: StrategyFactory) =
    _strategies += (name -> strategyFactory)

  def strategies: MMap[Symbol, ScentryStrategy[UserType]] =
    (globalStrategies ++ _strategies) map { case (nm, fact) => (nm -> fact.asInstanceOf[StrategyFactory](this)) }

  private var _winningStrategy: Option[Symbol] = None
  def winningStrategy = _winningStrategy

  def user = {
    runCallbacks { _.beforeFetch }
    val key = session.getAttribute(scentryAuthKey).asInstanceOf[String]
    val res = fromSession(key)
    runCallbacks { _.afterFetch }
    res
  }

  def user_=(v: UserType) = {
    strategies foreach { case (_, v) if v.valid_? => v.beforeSetUser }
    val res = toSession(v)
    session.setAttribute(scentryAuthKey, res)
    strategies foreach { case (_, v) if v.valid_? => v.afterSetUser }
    res
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
      runCallbacks { _.beforeAuthenticate }
      if(acc.isEmpty && strat.valid_? && (names.isEmpty || names.contains(nm))) {
        strat.authenticate_! match {
          case Some(usr)  => (nm, usr) :: acc
          case _ => acc
        }
       } else acc
    }.headOption match {
      case Some((stratName, usr)) =>  {
        _winningStrategy = Some(stratName)
        runCallbacks { _.afterAuthenticate }
        user = usr
      }
      case _ =>
    }
  }

  def logout = {
    runCallbacks { _.beforeLogout }
    session.invalidate
    runCallbacks { _.afterLogout }
  }

  private def runCallbacks(which: ScentryStrategy[UserType] => Unit ) {
    strategies foreach { case (_, v) if v.valid_? => which(v)}
  }
}