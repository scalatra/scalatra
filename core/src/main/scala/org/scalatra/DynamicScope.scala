package org.scalatra

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

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
 * This trick is explained in greater detail in Gabriele Renzi's blog
 * post about Step, out of which Scalatra grew:
 *
 * http://www.riffraff.info/2009/4/11/step-a-scala-web-picoframework
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

  protected def withRequestResponse[A](request: HttpServletRequest, response: HttpServletResponse)(f: => A) = {
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

  @deprecated("Do not invoke directly. Use `withRequest` to change the binding, or request to get the value", "2.1.0")
  protected def _request = dynamicRequest

  @deprecated("Do not invoke directly. Use `withResponse` to change the binding, or `response` to get the value", "2.1.0")
  protected def _response = dynamicResponse
}
