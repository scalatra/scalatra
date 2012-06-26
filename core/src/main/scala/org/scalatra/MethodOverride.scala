package org.scalatra

object MethodOverride {
  val ParamName = "_method"
  val HeaderName = "X-HTTP-METHOD-OVERRIDE"
}
/**
 * Mixin for clients that only support a limited set of HTTP verbs.  If the
 * request is a POST and the `_method` request parameter is set, the value of
 * the `_method` parameter is treated as the request's method.
 */
trait MethodOverride extends Handler {
  abstract override def handle(req: HttpRequest, res: HttpResponse) {
    val req2 = req.requestMethod match {
      case Post =>
        methodOverride(req) map { method =>
	        requestWithMethod(req, HttpMethod(method))
        } getOrElse req
      case _ =>
        req
    }
    super.handle(req2, res)
  }

  /**
   * Returns a request identical to the current request, but with the
   * specified method.
   *
   * For backward compatibility, we need to transform the underlying request
   * type to pass to the super handler.
   */
  protected def requestWithMethod(req: HttpRequest, method: HttpMethod): HttpRequest

  private def methodOverride(req: HttpRequest) = {
    import MethodOverride._
    (req.parameters.get(ParamName) orElse req.headers.get(HeaderName))
  }
}
