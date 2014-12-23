package org.scalatra

import java.lang.{ Integer => JInteger }

import scala.util.control.NoStackTrace

/**
 * A collection of methods that affect the control flow of routes.
 */
trait Control {
  /**
   * Immediately halts processing of a request.  Can be called from either a
   * before filter or a route.
   *
   * @param status the status to set on the response, or null to leave
   *        the status unchanged.
   * @param body a result to render through the render pipeline as the body
   * @param headers headers to add to the response
   * @param reason the HTTP status reason to set, or null to leave unchanged.
   */
  def halt[T: Manifest](
    status: JInteger = null,
    body: T = (),
    headers: Map[String, String] = Map.empty,
    reason: String = null): Nothing = {
    val statusOpt = if (status == null) None else Some(status.intValue)
    throw new HaltException(statusOpt, Some(reason), headers, body)
  }

  def halt(result: ActionResult): Nothing = {
    halt(result.status.code, result.body, result.headers, result.status.message)
  }

  /**
   * Immediately exits from the current route.
   */
  def pass(): Nothing = throw new PassException
}

private[scalatra] case class HaltException(
  status: Option[Int],
  reason: Option[String],
  headers: Map[String, String],
  body: Any)
    extends Throwable with NoStackTrace

private[scalatra] class PassException extends Throwable with NoStackTrace
