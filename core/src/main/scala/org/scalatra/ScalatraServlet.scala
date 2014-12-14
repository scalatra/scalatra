package org.scalatra

import servlet.ServletBase
import javax.servlet._
import javax.servlet.http._
import org.scalatra.util.RicherString._
import scala.util.control.Exception.catching

object ScalatraServlet {

  val RequestPathKey = "org.scalatra.ScalatraServlet.requestPath"
  import servlet.ServletApiImplicits._
  def requestPath(request: HttpServletRequest): String = {
    require(request != null, "The request can't be null for getting the request path")
    def startIndex(r: HttpServletRequest) =
      r.getContextPath.blankOption.map(_.length).getOrElse(0) + r.getServletPath.blankOption.map(_.length).getOrElse(0)
    def getRequestPath(r: HttpServletRequest) = {
      val u = (catching(classOf[NullPointerException]) opt { r.getRequestURI } getOrElse "/")
      requestPath(u, startIndex(r))
    }

    request.get(RequestPathKey) map (_.toString) getOrElse {
      val rp = getRequestPath(request)
      request(RequestPathKey) = rp
      rp
    }
  }

  def requestPath(uri: String, idx: Int): String = {
    val u1 = UriDecoder.firstStep(uri)
    val u2 = (u1.blankOption map { _.substring(idx) } flatMap (_.blankOption) getOrElse "/")
    val pos = u2.indexOf(';')
    if (pos > -1) u2.substring(0, pos) else u2
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
    require(config != null, "routeBasePath requires the servlet to be initialized")
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
