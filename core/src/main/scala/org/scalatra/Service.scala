package org.scalatra

trait Service { this: Backend =>
  def apply(implicit request: Request, response: Response): Option[Unit]
}
