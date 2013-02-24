package org.scalatra

import servlet.ServletBase
import javax.servlet._
import javax.servlet.http._
import org.scalatra.util.RicherString._
import scala.util.control.Exception.catching

object ScalatraServlet {
  import servlet.ServletApiImplicits._
  def requestPath(request: HttpServletRequest) = {
    require(request != null, "The request can't be null for getting the request path")
    def getRequestPath(r: HttpServletRequest) = {
      val u = (catching(classOf[NullPointerException]) opt { r.getRequestURI } getOrElse "/")
      val u2 = (u.blankOption map { uri =>
        val uu = if (r.getContextPath.nonBlank) uri.substring(r.getContextPath.length) else uri
        if (r.getServletPath.nonBlank) uu.substring(r.getServletPath.length) else uu
      } flatMap(_.blankOption) getOrElse "/")
      val pos = u2.indexOf(';')
      val u3 = if (pos > -1) u2.substring(0, pos) else u2
      UriDecoder.firstStep(u3)
    }

    request.get("org.scalatra.ScalatraServlet.requestPath") map (_.toString) getOrElse {
      val requestPath = getRequestPath(request)
      request("org.scalatra.ScalatraServlet.requestPath") = requestPath
      requestPath
    }
  }
}

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
  def requestPath(implicit request: HttpServletRequest) = ScalatraServlet.requestPath(request)

  protected def routeBasePath(implicit request: HttpServletRequest) = {
    require(config != null, "routeBasePath requires the servlet to be initialize")
    require(request != null, "routeBasePath requires an active request to determine the servlet path")

    servletContext.getContextPath + request.getServletPath
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

  override def destroy() {
    shutdown()
    super.destroy()
  }
}
