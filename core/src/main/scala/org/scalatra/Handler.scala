package org.scalatra

/**
 * A `Handler` is the Scalatra abstraction for an object that operates on
 * a request/response pair.  The request and response types themselves are
 * abstract in order to support multiple server adapters.
 */
trait Handler {


  /**
   * Handles a request and writes to the response.
   */
  def handle(request: HttpRequest, res: HttpResponse): Unit
}
