package org.scalatra

import http.{HttpRequest, HttpResponse}

trait Service extends RequestResponse {
  def apply(implicit request: Request, response: Response): Option[Unit]
}
