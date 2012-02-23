package org.scalatra

import http.{HttpRequest, HttpResponse}

trait RequestResponse {
  type Request

  protected def httpRequest: HttpRequest[Request]

  type Response

  protected def httpResponse: HttpResponse[Response]
}
