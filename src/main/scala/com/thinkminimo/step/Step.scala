package com.thinkminimo.step

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.mutable.HashSet
import scala.collection.jcl.Conversions._
import scala.xml.NodeSeq
import Session._

abstract class Step extends HttpServlet with StepKernel
{
  import StepKernel._

  override def service(request: HttpServletRequest, response: HttpServletResponse) = handle(request, response)

  // getPathInfo returns everything after the context path, so step will work if non-root
  protected def requestPath = request.getPathInfo

  protected var doNotFound: Action = () => {
    // TODO - We should return a 405 if the route matches a different method
    response.setStatus(404)
    response.getWriter println "Requesting %s but only have %s".format(request.getRequestURI, Routes)
  }
}
