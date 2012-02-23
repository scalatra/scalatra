package org.scalatra

import http.{HttpRequest, HttpResponse}

trait Service {
  type Request

  protected def httpRequest: HttpRequest[Request]

  type Response

  protected def httpResponse: HttpResponse[Response]

  def apply(request: Request, response: Response): Option[Unit]
}
