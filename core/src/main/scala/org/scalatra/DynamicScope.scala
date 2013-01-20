package org.scalatra

import scala.util.DynamicVariable
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

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

trait DynamicScope extends RequestResponseScope {
  /**
   * The currently scoped request.  Valid only inside the `handle` method.
   */
  implicit def request: HttpServletRequest = currentRequest

  private[this] var currentRequest: HttpServletRequest = null

  /**
   * The currently scoped response.  Valid only inside the `handle` method.
   */
  implicit def response: HttpServletResponse = currentResponse

  private[this] var currentResponse: HttpServletResponse = null

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
  protected def withRequest[A](request: HttpServletRequest)(f: => A) = {
    val oldRequest = currentRequest
    try {
      currentRequest = request
      f
    }
    finally {
      currentRequest = oldRequest
    }
  }

  /**
   * Executes the block with the given response bound to the `response`
   * method.
   */
  protected def withResponse[A](response: HttpServletResponse)(f: => A) = {
    val oldResponse = currentResponse
    try {
      currentResponse = response
      f
    }
    finally {
      currentResponse = oldResponse
    }
  }
}
