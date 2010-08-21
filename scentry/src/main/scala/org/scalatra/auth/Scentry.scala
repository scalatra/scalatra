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
protected[auth] class Scentry[UserType <: AnyRef](
        app: ScalatraKernelProxy,
        serialize: PartialFunction[UserType, String],
        deserialize: PartialFunction[String, UserType] ) {

  type StrategyType = ScentryStrategy[UserType]
  type StrategyFactory = ScalatraKernelProxy => StrategyType

  import Scentry._
  private val _strategies = new HashMap[Symbol, StrategyFactory]()

  def session = app.session
  def params = app.params
  def redirect(uri: String) = app.redirect(uri)

  def registerStrategy(name: Symbol, strategyFactory: StrategyFactory) =
    _strategies += (name -> strategyFactory)

  def strategies: MMap[Symbol, ScentryStrategy[UserType]] =
    (globalStrategies ++ _strategies) map { case (nm, fact) => (nm -> fact.asInstanceOf[StrategyFactory](app)) }

  private var _winningStrategy: Option[Symbol] = None
  def winningStrategy = _winningStrategy

  def user = {
    runCallbacks() { _.beforeFetch }
    val key = session.getAttribute(scentryAuthKey).asInstanceOf[String]
    val res = fromSession(key)
    runCallbacks() { _.afterFetch }
    res
  }

  def user_=(v: UserType) = {
    runCallbacks() { _.beforeSetUser }
    val res = toSession(v)
    session.setAttribute(scentryAuthKey, res)
    runCallbacks() { _.afterSetUser }
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
      runCallbacks(_.valid_?) { _.beforeAuthenticate }
      if(acc.isEmpty && strat.valid_? && (names.isEmpty || names.contains(nm))) {
        strat.authenticate_! match {
          case Some(usr)  => (nm, usr) :: acc
          case _ => acc
        }
       } else acc
    }.headOption match {
      case Some((stratName, usr)) =>  {
        _winningStrategy = Some(stratName)
        runCallbacks(_.valid_?) { _.afterAuthenticate }
        user = usr
      }
      case _ =>
    }
  }

  def logout = {
    runCallbacks() { _.beforeLogout }
    session.invalidate
    runCallbacks() { _.afterLogout }
  }

  private def runCallbacks(guard: StrategyType => Boolean = s => true)(which: StrategyType => Unit) {
    strategies foreach { case (_, v) if guard(v) => which(v)}
  }
}