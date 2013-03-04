package org.scalatra.commands

import scalaz._
import Scalaz._
import org.scalatra.validation._
import grizzled.slf4j.Logger
import mojolly.inflector.InflectorImports._
import annotation.implicitNotFound
import scala.util.{Failure => Fail, Success => Succ, Try}
import scalaz.Failure
import scala.concurrent.{ExecutionContext, Future}

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
  @transient private[this] val logger = Logger(getClass)

  def execute(cmd: T): ModelValidation[S] = {
    logger.debug(s"Executing [${cmd.getClass.getName}].\n$cmd")
    if (cmd.isValid) {
      val res = Try(handle(cmd))

      res match {
        case Succ(r) ⇒
          val resultLog = r.fold(
            { failures ⇒ s"with ${failures.tail.size + 1} failures\n${failures.list}" },
            { _ ⇒ "successfully" })
          logger.debug(s"Command [${cmd.getClass.getName}] executed $resultLog")
          r
        case Fail(t) ⇒
          logger.error(s"Command [${cmd.getClass.getName}] failed.", t)
          ValidationError(s"Failed to execute ${cmd.getClass.getSimpleName.underscore.humanize}", UnknownError).failNel[S]
      }
    } else {
      val f = cmd.errors.map(_.validation) collect {
        case Failure(e) ⇒ e
      }
      def failures = if (f.size == 1) "failure" else "failures"
      logger.debug(s"Command [${cmd.getClass.getName}}] executed with ${f.size} $failures.\n${f.toList}")
      NonEmptyList(f.head, f.tail: _*).fail
    }
  }
}

class BlockingCommandExecutor[T <: Command, S](handle: T => ModelValidation[S]) extends BlockingExecutor(handle)

class BlockingModelExecutor[T <: Command <% S, S](handle: S => ModelValidation[S]) extends BlockingExecutor[T, S](handle(_))

abstract class AsyncExecutor[T <: Command, S](handle: T => Future[ModelValidation[S]])(implicit executionContext: ExecutionContext) extends CommandExecutor[T, Future[ModelValidation[S]]](handle) {
  @transient private[this] val logger = Logger(getClass)
  def execute(cmd: T): Future[ModelValidation[S]] = {
    logger.debug(s"Executing [${cmd.getClass.getName}].\n$cmd")
    if (cmd.isValid) {
      val res = handle(cmd)

      res onSuccess {
        case r ⇒
          val resultLog = r.fold(
            { failures ⇒ s"with ${failures.list.size} ${"failure".plural(failures.list.size)}.\n${failures.list}" },
            { _ ⇒ "successfully" })
          logger.debug(s"Command [${cmd.getClass.getName}] executed $resultLog")
      }

      res recover {
        case t: Throwable =>
          logger.error(s"Command [${cmd.getClass.getName}] failed.", t)
          ValidationError(s"Failed to execute ${cmd.getClass.getSimpleName.underscore.humanize}", UnknownError).failNel[S]
      }
    } else {
      val f = cmd.errors.map(_.validation) collect {
        case Failure(e) ⇒ e
      }
      logger.debug(s"Command [${cmd.getClass.getName}] executed with ${f.size} ${"failure".plural(f.size)}.\n${f.toList}")
      Future.successful(NonEmptyList(f.head, f.tail: _*).fail)
    }
  }
}

class AsyncCommandExecutor[T <: Command, S](handle: T => Future[ModelValidation[S]])(implicit executionContext: ExecutionContext) extends AsyncExecutor[T, S](handle)(executionContext)
class AsyncModelExecutor[T <: Command, S](handle: S => Future[ModelValidation[S]])(implicit executionContext: ExecutionContext, vw: T => S) extends AsyncExecutor[T, S](handle(_))(executionContext)
