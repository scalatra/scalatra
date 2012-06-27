package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import servlet.{ServletRequest, ServletResponse}

/**
 * A `Handler` is the Scalatra abstraction for an object that operates on
 * a request/response pair.  The request and response types themselves are
 * abstract in order to support multiple server adapters.
 */
trait Handler {
  /**
   * Handles a request and writes to the response.
   */
  def handle(request: HttpServletRequest, res: HttpServletResponse): Unit

  protected implicit def enrichRequest(req: HttpServletRequest): ServletRequest = ServletRequest(req)
  
  protected implicit def enrichResponse(res: HttpServletResponse): ServletResponse
 = ServletResponse(res)
}
