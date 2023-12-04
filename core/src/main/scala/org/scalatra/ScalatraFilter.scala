package org.scalatra

import jakarta.servlet._
import jakarta.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.servlet.ServletBase
import org.scalatra.util.RicherString._

import scala.util.control.Exception.catching
import scala.util.DynamicVariable

/**
 * An implementation of the Scalatra DSL in a filter.  You may prefer a filter
 * to a ScalatraServlet if:
 *
 * $ - you are sharing a URL space with another servlet or filter and want to
 *     delegate unmatched requests.  This is very useful when migrating
 *     legacy applications one page or resource at a time.
 *
 *
 * Unlike a ScalatraServlet, does not send 404 or 405 errors on non-matching
 * routes.  Instead, it delegates to the filter chain.
 *
 * If in doubt, extend ScalatraServlet instead.
 *
 * @see ScalatraServlet
 */
trait ScalatraFilter extends Filter with ServletBase {

  private[this] val _filterChain: DynamicVariable[FilterChain] = new DynamicVariable[FilterChain](null)

  val RequestPathKey = "org.scalatra.ScalatraFilter.requestPath"

  protected def filterChain: FilterChain = _filterChain.value

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    val httpRequest = request.asInstanceOf[HttpServletRequest]
    val httpResponse = response.asInstanceOf[HttpServletResponse]

    _filterChain.withValue(chain) {
      handle(httpRequest, httpResponse)
    }
  }

  // What goes in servletPath and what goes in pathInfo depends on how the underlying servlet is mapped.
  // Unlike the Scalatra servlet, we'll use both here by default.  Don't like it?  Override it.
  def requestPath(implicit request: HttpServletRequest): String = {
    require(request != null, "The request can't be null for getting the request path")
    def startIndex(r: HttpServletRequest) =
      r.getContextPath.blankOption.map(_.length).getOrElse(0)
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
      val u1 = if (pos >= 0) uri.substring(0, pos) else uri
      val u2 = if (decodePercentEncodedPath) UriDecoder.decode(u1) else u1
      u2.substring(idx).blankOption.getOrElse("/")
    }
  }

  protected def routeBasePath(implicit request: HttpServletRequest): String = {
    if (servletContext == null)
      throw new IllegalStateException("routeBasePath requires an initialized servlet context to determine the context path")
    servletContext.getContextPath
  }

  protected var doNotFound: Action = () => filterChain.doFilter(request, response)

  methodNotAllowed { _ => filterChain.doFilter(request, response) }

  type ConfigT = FilterConfig

  // see Initializable.initialize for why
  override def init(filterConfig: FilterConfig): Unit = {
    initialize(filterConfig)
  }

  override def destroy: Unit = {
    shutdown()
  }

}
