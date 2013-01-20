package org.scalatra

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

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
