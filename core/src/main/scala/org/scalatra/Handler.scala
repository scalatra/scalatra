package org.scalatra

/**
 * A `Handler` is the Scalatra abstraction for an object that operates on
 * a request/response pair.
 */
trait Handler {
  /**
   * Handles a request and writes to the response.
   */
  def handle(request: HttpRequest, res: HttpResponse): Unit
}
