package org.scalatra

import org.scalatra.ServletCompat.*
import org.scalatra.ServletCompat.http.*

import org.apache.commons.text.StringEscapeUtils
import org.scalatra.servlet.ServletBase
import org.scalatra.util.RicherString.*

import scala.util.control.Exception.catching

/** An implementation of the Scalatra DSL in a servlet. This is the recommended base trait for most Scalatra
  * applications. Use a servlet if:
  *
  * $ - your Scalatra routes run in a subcontext of your web application. $ - you want Scalatra to have complete control
  * of unmatched requests. $ - you think you want a filter just for serving static content with the default servlet;
  * ScalatraServlet can do this too $ - you don't know the difference
  *
  * @see
  *   ScalatraFilter
  */
trait ScalatraServlet extends HttpServlet with ServletBase with Initializable {

  override def service(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    handle(request, response)
  }

  /** Defines the request path to be matched by routers. The default definition is optimized for `path mapped` servlets
    * (i.e., servlet mapping ends in `&#47;*`). The route should match everything matched by the `&#47;*`. In the event
    * that the request URI equals the servlet path with no trailing slash (e.g., mapping = `/admin&#47;*`, request URI =
    * '/admin'), a '/' is returned.
    *
    * All other servlet mappings likely want to return request.getServletPath. Custom implementations are allowed for
    * unusual cases.
    */
  val RequestPathKey = "org.scalatra.ScalatraServlet.requestPath"

  def requestPath(implicit request: HttpServletRequest): String = {
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
    if (uri.length == 0) {
      "/"
    } else {
      val pos = uri.indexOf(';')
      val u1  = if (pos >= 0) uri.substring(0, pos) else uri
      val u2  = if (decodePercentEncodedPath) UriDecoder.decode(u1) else u1
      u2.substring(idx).blankOption.getOrElse("/")
    }
  }

  protected def routeBasePath(implicit request: HttpServletRequest): String = {
    require(config != null, "routeBasePath requires the servlet to be initialized")
    require(request != null, "routeBasePath requires an active request to determine the servlet path")

    servletContext.getContextPath + request.getServletPath
  }

  /** Invoked when no route matches. By default, calls `serveStaticResource()`, and if that fails, calls
    * `resourceNotFound()`.
    *
    * This action can be overridden by a notFound block.
    */
  protected var doNotFound: Action = () => {
    serveStaticResource() getOrElse resourceNotFound()
  }

  /** Attempts to find a static resource matching the request path. Override to return None to stop this.
    */
  protected def serveStaticResource()(implicit
      request: HttpServletRequest,
      response: HttpServletResponse
  ): Option[Any] = {
    servletContext.resource(request) map { _ =>
      servletContext.getNamedDispatcher("default").forward(request, response)
    }
  }

  /** Called by default notFound if no routes matched and no static resource could be found.
    */
  protected def resourceNotFound()(implicit request: HttpServletRequest, response: HttpServletResponse): Any = {
    response.setStatus(404)
    if (isDevelopmentMode) {
      val error = "Requesting \"%s %s\" on servlet \"%s\" but only have: %s"
      response.getWriter println error.format(
        request.getMethod,
        Option(StringEscapeUtils.escapeHtml4(request.getPathInfo)) getOrElse "/",
        request.getServletPath,
        routes.entryPoints.mkString("<ul><li>", "</li><li>", "</li></ul>")
      )
    }
  }

  type ConfigT = ServletConfig

  override def init(config: ServletConfig): Unit = {
    super.init(config)
    initialize(config) // see Initializable.initialize for why
  }

  override def initialize(config: ServletConfig): Unit = {
    super.initialize(config)
  }

  override def destroy(): Unit = {
    shutdown()
    super.destroy()
  }

}
