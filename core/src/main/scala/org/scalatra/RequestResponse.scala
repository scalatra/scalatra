package org.scalatra

import http.{HttpRequest, HttpResponse}

trait RequestResponse {
  type Request >: Null
  protected implicit def requestWrapper(request: Request): HttpRequest

  type Response >: Null
  protected implicit def responseWrapper(response: Response): HttpResponse
}
