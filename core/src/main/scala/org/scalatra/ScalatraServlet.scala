package org.scalatra

import servlet.ServletBase
import javax.servlet._
import javax.servlet.http._

/**
 * An implementation of the Scalatra DSL in a servlet.  This is the recommended
 * base class for most Scalatra applications.  Use a servlet if:
 *
 * $ - your Scalatra routes run in a subcontext of your web application.
 * $ - you want Scalatra to have complete control of unmatched requests.
 * $ - you think you want a filter just for serving static content with the
 *     default servlet; ScalatraServlet can do this too
 * $ - you don't know the difference
 *
 * @see ScalatraFilter
 */
abstract class ScalatraServlet
  extends HttpServlet
  with ServletBase
  with Initializable {
  override def service(request: HttpServletRequest, response: HttpServletResponse) {
    handle(request, response)
  }

  /**
   * Defines the request path to be matched by routers.  The default
   * definition is optimized for `path mapped` servlets (i.e., servlet
   * mapping ends in `&#47;*`).  The route should match everything matched by
   * the `&#47;*`.  In the event that the request URI equals the servlet path
   * with no trailing slash (e.g., mapping = `/admin&#47;*`, request URI =
   * '/admin'), a '/' is returned.
   *
   * All other servlet mappings likely want to return request.getServletPath.
   * Custom implementations are allowed for unusual cases.
   */
  def requestPath = {
    def getRequestPath = request.getRequestURI match {
      case requestURI: String =>
        var uri = requestURI
        if (request.getContextPath != null && request.getContextPath.trim.nonEmpty) uri = uri.substring(request.getContextPath.length)
        if (request.getServletPath != null && request.getServletPath.trim.nonEmpty) uri = uri.substring(request.getServletPath.length)
        if (uri.isEmpty) {
          uri = "/"
        } else {
          val pos = uri.indexOf(';')
          if (pos >= 0) uri = uri.substring(0, pos)
        }
        UriDecoder.firstStep(uri)
      case null => "/"
    }

    request.get("org.scalatra.ScalatraServlet.requestPath") match {
      case Some(uri) => uri.toString
      case _         => {
        val requestPath = getRequestPath
        request.setAttribute("org.scalatra.ScalatraServlet.requestPath", requestPath)
        requestPath.toString
      }
    }
  }

  protected def routeBasePath = {
    if (request == null)
      throw new IllegalStateException("routeBasePath requires an active request to determine the servlet path")
    request.getContextPath + request.getServletPath
  }

  /**
   * Invoked when no route matches.  By default, calls `serveStaticResource()`,
   * and if that fails, calls `resourceNotFound()`.
   *
   * This action can be overridden by a notFound block.
   */
  protected var doNotFound: Action = () => {
    serveStaticResource() getOrElse resourceNotFound()
  }

  /**
   * Attempts to find a static resource matching the request path.  Override
   * to return None to stop this.
   */
  protected def serveStaticResource(): Option[Any] =
    servletContext.resource(request) map { _ =>
      servletContext.getNamedDispatcher("default").forward(request, response)
    }

  /**
   * Called by default notFound if no routes matched and no static resource
   * could be found.
   */
  protected def resourceNotFound(): Any = {
    response.setStatus(404)
    if (isDevelopmentMode) {
      val error = "Requesting \"%s %s\" on servlet \"%s\" but only have: %s"
      response.getWriter println error.format(
        request.getMethod,
        Option(request.getPathInfo) getOrElse "/",
        request.getServletPath,
        routes.entryPoints.mkString("<ul><li>", "</li><li>", "</li></ul>"))
    }
  }

  type ConfigT = ServletConfig

  override def init(config: ServletConfig) {
    super.init(config)
    initialize(config) // see Initializable.initialize for why
  }

  override def initialize(config: ServletConfig) {
    super.initialize(config)
  }
}
