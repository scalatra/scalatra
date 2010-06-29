package com.thinkminimo.step

import scala.util.DynamicVariable
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import javax.servlet._

trait StepFilter extends Filter with StepKernel {
  import StepKernel._

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
  // Unlike the Step servlet, we'll use both here by default.  Don't like it?  Override it.
  protected def requestPath = request.getServletPath + (if (request.getPathInfo != null) request.getPathInfo else "")

  protected var doNotFound: Action = () => filterChain.doFilter(request, response)

  // must be a var because we don't know its value until the servlet container calls init 
  protected var servletContext: ServletContext = _

  def init(filterConfig: FilterConfig) = {
    servletContext = filterConfig.getServletContext
  }

  def destroy = {}
}