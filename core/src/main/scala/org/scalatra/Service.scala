package org.scalatra

trait Service {
  type Request
  type Response

  def apply(request: Request, response: Response): Option[Unit]
}
