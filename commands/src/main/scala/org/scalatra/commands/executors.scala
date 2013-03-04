package org.scalatra.commands

import scalaz._
import Scalaz._
import akka.dispatch.{Promise, ExecutionContext, Future}
import org.scalatra.validation._
import grizzled.slf4j
import scala.util.control.Exception.allCatch
import mojolly.inflector.InflectorImports._
import annotation.implicitNotFound

trait CommandExecutors {

  implicit def syncExecutor[T <: Command, S](handler: T => ModelValidation[S]): CommandExecutor[T, ModelValidation[S]] =
    new BlockingCommandExecutor(handler)

  implicit def syncModelExecutor[T <: Command <% S, S](handler: S => ModelValidation[S]): CommandExecutor[T, ModelValidation[S]] =
    new BlockingModelExecutor[T, S](handler)

  implicit def asyncExecutor[T <: Command, S](handler: T => Future[ModelValidation[S]])(implicit executionContext: ExecutionContext): CommandExecutor[T, Future[ModelValidation[S]]] =
    new AsyncCommandExecutor(handler)

  implicit def asyncModelExecutor[T <: Command, S](handler: S => Future[ModelValidation[S]])(implicit executionContext: ExecutionContext, vw: T => S): CommandExecutor[T, Future[ModelValidation[S]]] =
    new AsyncModelExecutor[T, S](handler)

}

object CommandExecutors extends CommandExecutors


@implicitNotFound(
  "Couldn't find an executor for command of type ${T} and result of type ${S}. Did you import org.scalatra.commands.CommandExecutors._ ? You can also implement your own org.scalatra.CommandExecutor."
)
abstract class CommandExecutor[T <: Command, S](handler: T => S) {
  def execute(command: T): S
}

abstract class BlockingExecutor[T <: Command, S](handle: T => ModelValidation[S]) extends CommandExecutor[T, ModelValidation[S]](handle) {

  @transient private[this] val logger = slf4j.Logger(getClass)

  def execute(cmd: T): ModelValidation[S] = {
    logger.debug("Executing ["+cmd.getClass.getName+"].\n"+cmd)
    if (cmd.isValid) {
      val res = allCatch.either(handle(cmd))

      res match {
        case Right(r) ⇒
          val resultLog = r.fold(
            { failures ⇒ "with %d failures.\n%s".format(failures.tail.size + 1, failures.list) },
            { _ ⇒ "successfully." })
          logger.debug("Command ["+cmd.getClass.getName+"] executed "+resultLog)
          r
        case Left(t) ⇒
          logger.error("Command ["+cmd.getClass.getName+"] failed.", t)
          ValidationError("Failed to execute "+cmd.getClass.getSimpleName.underscore.humanize, UnknownError).failNel[S]
      }
    } else {
      val f = cmd.errors.map(_.validation) collect { case Failure(e) ⇒ e }
      def failures = if (f.size == 1) "failure".singularize else "failure".pluralize
      logger.debug("Command ["+cmd.getClass.getName+" executed with "+f.size+" "+failures+".\n"+f.toList)
      NonEmptyList(f.head, f.tail: _*).fail
    }
  }
}

class BlockingCommandExecutor[T <: Command, S](handle: T => ModelValidation[S]) extends BlockingExecutor(handle)

class BlockingModelExecutor[T <: Command <% S, S](handle: S => ModelValidation[S]) extends BlockingExecutor[T, S](handle(_))

abstract class AsyncExecutor[T <: Command, S](handle: T => Future[ModelValidation[S]])(implicit executionContext: ExecutionContext) extends CommandExecutor[T, Future[ModelValidation[S]]](handle) {
  @transient private[this] val logger = slf4j.Logger(getClass)
  def execute(cmd: T): Future[ModelValidation[S]] = {
    logger.debug("Executing ["+cmd.getClass.getName+"].\n"+cmd)
    if (cmd.isValid) {
      val res = handle(cmd)

      res onSuccess {
        case r ⇒
          val resultLog = r.fold(
            { failures ⇒ "with %d failures.\n%s".format(failures.tail.size + 1, failures.list) },
            { _ ⇒ "successfully." })
          logger.debug("Command ["+cmd.getClass.getName+"] executed "+resultLog)
      }

      res recover {
        case t: Throwable =>
          logger.error("Command ["+cmd.getClass.getName+"] failed.", t)
          ValidationError("Failed to execute "+cmd.getClass.getSimpleName.underscore.humanize, UnknownError).failNel[S]
      }
    } else {
      val f = cmd.errors.map(_.validation) collect { case Failure(e) ⇒ e }
      def failures = if (f.size == 1) "failure".singularize else "failure".pluralize
      logger.debug("Command ["+cmd.getClass.getName+" executed with "+f.size+" "+failures+".\n"+f.toList)
      Promise.successful(NonEmptyList(f.head, f.tail: _*).fail).future
    }
  }
}

class AsyncCommandExecutor[T <: Command, S](handle: T => Future[ModelValidation[S]])(implicit executionContext: ExecutionContext) extends AsyncExecutor[T, S](handle)(executionContext)
class AsyncModelExecutor[T <: Command, S](handle: S => Future[ModelValidation[S]])(implicit executionContext: ExecutionContext, vw: T => S) extends AsyncExecutor[T, S](handle(_))(executionContext)
