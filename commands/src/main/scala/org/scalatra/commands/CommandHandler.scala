package org.scalatra
package commands

import scala.util.control.Exception.allCatch
import grizzled.slf4j.Logger
import validation._
import scalaz._
import Scalaz._

trait CommandHandler { 
  @transient private[this] val commandLogger: Logger = Logger[this.type]
  def execute[S: Manifest](cmd: ModelCommand[S]): ModelValidation[S] = {
    commandLogger.debug("Executing [%s].\n%s" format (cmd.getClass.getName, cmd))
    if (cmd.isValid) {
      val res = (allCatch withApply (serverError(cmd.getClass.getName, _))) {
        handle.lift(cmd).map(_.map(_.asInstanceOf[S])) | ValidationError("Don't know how to handle: " + cmd.getClass.getName, UnknownError).failNel
      }

      val resultLog = res.fold(
        { failures => "with %d failures\n%s".format(failures.tail.size + 1, failures.list) },
        { _ => "successfully" }
      )
      commandLogger.debug("Command [%s] executed %s." format (cmd.getClass.getName, resultLog))
      res
    } else {
      val f = cmd.errors.map(_.validation) collect {
        case Failure(e) ⇒ e
      }
      commandLogger.debug("Command [%s] executed with %d failures.\n%s" format (cmd.getClass.getName, f.size, f.toList))
      NonEmptyList(f.head, f.tail: _*).fail
    }
  }

  private[this] def serverError[R](cmdName: String, ex: Throwable): ModelValidation[R] = {
    commandLogger.error("There was an error while executing " + cmdName, ex)
    ValidationError("An error occurred while handling: " + cmdName, UnknownError).failNel[R]
  }

  type Handler = PartialFunction[ModelCommand[_], ModelValidation[_]]

  protected def handle: Handler
}
