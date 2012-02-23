package org.scalatra

import http.{HttpRequest, HttpResponse}

trait Service { this: Backend =>
  def apply(implicit request: Request, response: Response): Option[Unit]
}
