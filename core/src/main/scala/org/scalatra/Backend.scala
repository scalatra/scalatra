package org.scalatra

trait Backend {
  type Request >: Null
  protected implicit def requestWrapper(request: Request): HttpRequest

  type Response >: Null
  protected implicit def responseWrapper(response: Response): HttpResponse

  type Context
  /**
   * This is called servletContextWrapper for historical reasons
   */
  protected implicit def servletContextWrapper(context: Context): ApplicationContext
}
