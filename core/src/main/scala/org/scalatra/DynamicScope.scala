package org.scalatra

import scala.util.DynamicVariable

/**
 * The Scalatra DSL requires a dynamically scoped request and response.
 * This trick is explained in greater detail in Gabriele Renzi's blog
 * post about Step, out of which Scalatra grew:
 *
 * http://www.riffraff.info/2009/4/11/step-a-scala-web-picoframework
 */
trait DynamicScope {
  type RequestT >: Null <: Request
  type ResponseT >: Null <: Response

  /**
   * The currently scoped request.  Valid only inside the `handle` method.
   */
  implicit def request: RequestT = dynamicRequest.value

  private[this] val dynamicRequest = new DynamicVariable[RequestT](null)

  /**
   * The currently scoped response.  Valid only inside the `handle` method.
   */
  implicit def response: ResponseT = dynamicResponse.value

  private[this] val dynamicResponse = new DynamicVariable[ResponseT](null)

  protected def withRequestResponse[A](request: RequestT, response: ResponseT)(f: => A) = {
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
  protected def withRequest[A](request: RequestT)(f: => A) = 
    dynamicRequest.withValue(request) {
      f
    }

  /**
   * Executes the block with the given response bound to the `response`
   * method.
   */
  protected def withResponse[A](response: ResponseT)(f: => A) =
    dynamicResponse.withValue(response) {
      f
    }

  @deprecated("Do not invoke directly. Use `withRequest` to change the binding, or request to get the value", "2.1.0")
  protected def _request = dynamicRequest

  @deprecated("Do not invoke directly. Use `withResponse` to change the binding, or `response` to get the value", "2.1.0")
  protected def _response = dynamicResponse
}
