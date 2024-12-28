package org.scalatra

import java.lang.{Integer => JInteger}

import scala.util.control.NoStackTrace

/** A collection of methods that affect the control flow of routes.
  */
trait Control {

  /** Immediately halts processing of a request. Can be called from either a
    * before filter or a route.
    *
    * @param status
    *   the status to set on the response, or null to leave the status
    *   unchanged.
    * @param body
    *   a result to render through the render pipeline as the body
    * @param headers
    *   headers to add to the response
    */
  def halt[T](
      status: JInteger = null,
      body: T = (),
      headers: Map[String, String] = Map.empty
  ): Nothing = {
    val statusOpt = if (status == null) None else Some(status.intValue)
    throw new HaltException(statusOpt, headers, body)
  }

  def halt(result: ActionResult): Nothing =
    halt(result.status, result.body, result.headers)

  /** Immediately exits from the current route.
    */
  def pass(): Nothing = throw new PassException
}

case class HaltException(
    status: Option[Int],
    headers: Map[String, String],
    body: Any
) extends Throwable
    with NoStackTrace

class PassException extends Throwable with NoStackTrace
