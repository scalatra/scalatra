package org.scalatra
package databinding

import util.{ParamsValueReaderProperties, MultiMap}
import _root_.scalaz._
import Scalaz._
import collection.mutable
import java.util.Date
import org.joda.time.DateTime
import collection.JavaConverters._
import java.util.concurrent.ConcurrentHashMap
import grizzled.slf4j.Logger

/**
* Support for [[org.scalatra.databinding.Command]] binding and validation.
*/
trait CommandSupport extends ParamsValueReaderProperties { this: ScalatraBase =>

  type CommandType <: Command
  
  private[this] val commandFactories: mutable.ConcurrentMap[Class[_], () => Command] = new ConcurrentHashMap[Class[_], () => Command].asScala
  private[this] val logger: Logger = Logger[this.type]
  
  def registerCommand[T <: Command](cmd: => T)(implicit mf: Manifest[T]) {
    commandFactories += (mf.erasure -> (() => cmd))
  }

  /**
   * Create and bind a [[org.scalatra.databinding.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  def command[T <: CommandType](implicit mf: Manifest[T]): T = {
    def createCommand = commandFactories.get(mf.erasure).map(_()).getOrElse(mf.erasure.newInstance()).asInstanceOf[T]
    commandOption[T] getOrElse bindCommand(createCommand)
  }

  /**
   * Create and bind a [[org.scalatra.databinding.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  def commandOrElse[T <: CommandType](factory: ⇒ T)(implicit mf: Manifest[T]): T = {
    commandOption[T] getOrElse bindCommand(factory)
  }

  protected def bindCommand[T <: CommandType](newCommand: T)(implicit mf: Manifest[T]): T = {
    logger debug  "binding command: %s from %s".format(mf.erasure.getSimpleName, "html")
    newCommand.bindTo(params, multiParams, request.headers)
    requestProxy.update(commandRequestKey[T], newCommand)
    newCommand
  }

  def commandOption[T <: CommandType : Manifest] : Option[T] = requestProxy.get(commandRequestKey[T]).map(_.asInstanceOf[T])

  private[databinding] def requestProxy: mutable.Map[String, Any] = request

  private[databinding] def commandRequestKey[T <: CommandType : Manifest] = "_command_" + manifest[T].erasure.getName

  private class CommandRouteMatcher[T <: CommandType ](implicit mf: Manifest[T]) extends RouteMatcher {

    override def apply(requestPath: String) = if (command[T].isValid) Some(MultiMap()) else None
  }

  /**
   * Create a [[org.scalatra.RouteMatcher]] that evaluates '''true''' only if a command is valid. See
   * [[org.scalatra.databinding.validation.ValidationSupport]] for details.
   */
  def ifValid[T <: CommandType](implicit mf: Manifest[T]): RouteMatcher = new CommandRouteMatcher[T]


}

trait ParamsOnlyCommandSupport extends CommandSupport { this: ScalatraBase =>
  type CommandType = ParamsOnlyCommand
}
