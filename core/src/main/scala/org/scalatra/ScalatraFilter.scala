package org.scalatra

import scala.util.DynamicVariable
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.servlet._

/**
 * An implementation of the Scalatra DSL in a filter.  You may prefer a filter
 * to a ScalatraServlet if:
 *
 * $ - you are sharing a URL space with another servlet or filter and want to
 *     delegate unmatched requests.  This is very useful when migrating
 *     legacy applications one page or resource at a time.
 *
 * If in doubt, extend ScalatraServlet instead.
 *
 * @see ScalatraServlet
 */
trait ScalatraFilter extends Filter with ScalatraKernel with Initializable {
  import ScalatraKernel._

  private val _filterChain = new DynamicVariable[FilterChain](null)
  protected def filterChain = _filterChain.value

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) = {
    val httpRequest = request.asInstanceOf[HttpServletRequest]
    val httpResponse = response.asInstanceOf[HttpServletResponse]

    _filterChain.withValue(chain) {
      handle(httpRequest, httpResponse)
    }
  }

  // What goes in servletPath and what goes in pathInfo depends on how the underlying servlet is mapped.
  // Unlike the Scalatra servlet, we'll use both here by default.  Don't like it?  Override it.
  def requestPath = request.getServletPath + (if (request.getPathInfo != null) request.getPathInfo else "")

  protected var doNotFound: Action = () => filterChain.doFilter(request, response)

  var servletContext: ServletContext = _

  type Config = FilterConfig

  // see Initializable.initialize for why
  def init(filterConfig: FilterConfig) = initialize(filterConfig)

  override def initialize(config: FilterConfig): Unit = {
    super.initialize(config)
    servletContext = config.getServletContext
  }

  def destroy = {}
}
