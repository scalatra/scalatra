package org.scalatra

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._
import scala.xml.NodeSeq

abstract class ScalatraServlet 
  extends HttpServlet 
  with ScalatraKernel 
  with Initializable
{
  import ScalatraKernel._

  override def service(request: HttpServletRequest, response: HttpServletResponse) = handle(request, response)

  // pathInfo is for path-mapped servlets (i.e., the mapping ends in "/*").  Path-mapped Scalatra servlets will work even
  // if the servlet is not mapped to the context root.  Routes should contain everything matched by the "/*".
  //
  // If the servlet mapping is not path-mapped, then we fall back to the servletPath.  Routes should have a leading
  // slash and include everything between the context route and the query string.
  def requestPath = if (request.getPathInfo != null) request.getPathInfo else request.getServletPath

  protected var doNotFound: Action = () => {
    // TODO - We should return a 405 if the route matches a different method
    response.setStatus(404)
    response.getWriter println "Requesting %s but only have %s".format(request.getRequestURI, routes)
  }

  protected def servletContext: ServletContext = getServletContext

  type Config = ServletConfig

  private var _kernelName: String = _
  def kernelName = _kernelName

  override def init(config: ServletConfig) = {
    super.init(config)
    _kernelName = "servlet:"+config.getServletName
    initialize(config) // see Initializable.initialize for why
  }

  override def initialize(config: ServletConfig): Unit = super.initialize(config)
}
