//package org.scalatra
//package databinding
//
//import org.scalatra.json.JsonSupport
//import org.scalatra.validation.ValidationSupport
//import util.MultiMap
//import scalaz._
//import Scalaz._
//import collection.mutable
//
//
///**
// * Support for [[org.scalatra.command.Command]] binding and validation.
// */
//trait CommandSupport {
//
//  this: ScalatraBase with JsonSupport =>
//
//  type CommandValidatedType = Command with ValidationSupport
//
//  /**
//   * Implicitly convert a [[org.scalatra.command.Binding]] value to an [[scala.Option]]
//   */
//  implicit def bindingValue[T](b: Binding[T]): Option[T] = b.converted
//
//  /**
//   * Create and bind a [[org.scalatra.command.Command]] of the given type with the current Scalatra params.
//   *
//   * For every command type, creation and binding is performed only once and then stored into
//   * a request attribute.
//   */
//  def command[T <: Command](implicit mf: Manifest[T]): T = {
//    commandOption[T].getOrElse {
//      val newCommand = mf.erasure.newInstance.asInstanceOf[T]
//      newCommand.doBinding(params)
//      requestProxy.update(commandRequestKey[T], newCommand)
//      newCommand
//    }
//  }
//
//  def commandOption[T <: Command : Manifest] : Option[T] = requestProxy.get(commandRequestKey[T]).map(_.asInstanceOf[T])
//
//  private[databinding] def requestProxy: mutable.Map[String, AnyRef] = request
//
//  private[databinding] def commandRequestKey[T <: Command : Manifest] = "_command_" + manifest[T].erasure.getName
//
//  private class CommandRouteMatcher[T <: CommandValidatedType](implicit mf: Manifest[T]) extends RouteMatcher {
//
//    override def apply() = if (command[T].valid.getOrElse(true)) Some(MultiMap()) else None
//
//    override def toString = "[valid command guard]"
//  }
//
//  /**
//   * Create a [[org.scalatra.RouteMatcher]] that evaluates '''true''' only if a command is valid. See
//   * [[org.scalatra.command.validation.ValidationSupport]] for details.
//   */
//  def ifValid[T <: CommandValidatedType](implicit mf: Manifest[T]): RouteMatcher = new CommandRouteMatcher[T]
//}
