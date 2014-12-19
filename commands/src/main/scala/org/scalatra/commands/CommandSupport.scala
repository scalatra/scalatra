package org.scalatra
package commands

import util.{ ParamsValueReaderProperties, MultiMap }
import java.util.Date
import org.joda.time.DateTime
import collection.JavaConverters._
import java.util.concurrent.ConcurrentHashMap
import grizzled.slf4j.Logger
import scala.collection.concurrent.{ Map => ConcurrentMap }
import javax.servlet.http.HttpServletRequest
import scala.concurrent.{ Future, ExecutionContext }

/**
 * Support for [[org.scalatra.commands.Command]] binding and validation.
 */
trait CommandSupport extends ParamsValueReaderProperties with CommandExecutors { this: ScalatraBase =>

  type CommandType <: Command

  private[this] val commandFactories: ConcurrentMap[Class[_], () => Command] = new ConcurrentHashMap[Class[_], () => Command].asScala

  def registerCommand[T <: Command](cmd: => T)(implicit mf: Manifest[T]) {
    commandFactories += (mf.erasure -> (() => cmd))
  }

  /**
   * Create and bind a [[org.scalatra.commands.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  def command[T <: CommandType](implicit request: HttpServletRequest, mf: Manifest[T]): T = {
    def createCommand = commandFactories.get(mf.erasure).map(_()).getOrElse(mf.erasure.newInstance()).asInstanceOf[T]
    commandOption[T] getOrElse bindCommand(createCommand)
  }

  /**
   * Create and bind a [[org.scalatra.commands.Command]] of the given type with the current Scalatra params.
   *
   * For every command type, creation and binding is performed only once and then stored into
   * a request attribute.
   */
  def commandOrElse[T <: CommandType](factory: ⇒ T)(implicit request: HttpServletRequest, mf: Manifest[T]): T = {
    commandOption[T] getOrElse bindCommand(factory)
  }

  protected def bindCommand[T <: CommandType](newCommand: T)(implicit request: HttpServletRequest, mf: Manifest[T]): T = {
    newCommand.bindTo(params(request), multiParams(request), request.headers)
    newCommand
  }

  def commandOption[T <: CommandType](implicit request: HttpServletRequest, mf: Manifest[T]): Option[T] =
    request.get(commandRequestKey[T]).map(_.asInstanceOf[T])

  private[commands] def commandRequestKey[T <: CommandType](implicit request: HttpServletRequest, mf: Manifest[T]) =
    "_command_" + manifest[T].erasure.getName

  private class CommandRouteMatcher[T <: CommandType](implicit mf: Manifest[T]) extends RouteMatcher {

    override def apply(requestPath: String) = if (command[T].isValid) Some(MultiMap()) else None
  }

  /**
   * Create a [[org.scalatra.RouteMatcher]] that evaluates '''true''' only if a command is valid.
   */
  def ifValid[T <: CommandType](implicit mf: Manifest[T]): RouteMatcher = new CommandRouteMatcher[T]

}

trait ParamsOnlyCommandSupport extends CommandSupport { this: ScalatraBase =>
  type CommandType = ParamsOnlyCommand
}
