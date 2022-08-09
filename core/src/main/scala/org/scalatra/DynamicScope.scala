package org.scalatra

import jakarta.servlet.http.{ HttpServletRequest, HttpServletResponse }

import scala.util.DynamicVariable

trait RequestResponseScope {
  /**
   * The currently scoped request.  Valid only inside the `handle` method.
   */
  implicit def request: HttpServletRequest

  /**
   * The currently scoped response.  Valid only inside the `handle` method.
   */
  implicit def response: HttpServletResponse

  protected def withRequestResponse[A](request: HttpServletRequest, response: HttpServletResponse)(f: => A): A

  /**
   * Executes the block with the given request bound to the `request`
   * method.
   */
  protected def withRequest[A](request: HttpServletRequest)(f: => A): A

  /**
   * Executes the block with the given response bound to the `response`
   * method.
   */
  protected def withResponse[A](response: HttpServletResponse)(f: => A): A

}

/**
 * The Scalatra DSL requires a dynamically scoped request and response.
 */
trait DynamicScope extends RequestResponseScope {
  /**
   * The currently scoped request.  Valid only inside the `handle` method.
   */
  implicit def request: HttpServletRequest = dynamicRequest.value

  private[this] val dynamicRequest = new DynamicVariable[HttpServletRequest](null)

  /**
   * The currently scoped response.  Valid only inside the `handle` method.
   */
  implicit def response: HttpServletResponse = dynamicResponse.value

  private[this] val dynamicResponse = new DynamicVariable[HttpServletResponse](null)

  protected[scalatra] def withRequestResponse[A](request: HttpServletRequest, response: HttpServletResponse)(f: => A) = {
    withRequest(request) {
      withResponse(response) {
        f
      }
    }
  }

  /**
   * Executes the block with the given request bound to the `request`
   * method.
   */
  protected def withRequest[A](request: HttpServletRequest)(f: => A) =
    dynamicRequest.withValue(request) {
      f
    }

  /**
   * Executes the block with the given response bound to the `response`
   * method.
   */
  protected def withResponse[A](response: HttpServletResponse)(f: => A) =
    dynamicResponse.withValue(response) {
      f
    }
}
