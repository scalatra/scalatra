package com.thinkminimo.step

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.mutable.HashSet
import scala.collection.jcl.MapWrapper
import Session._

case class StepRequest(r: HttpServletRequest) {
  val p = new Regex("([^:]*):?(.*)")

  val List(host, port) = r.getHeader("Host") match { case null => List("",""); case p(x,y) => List(x,y) }
  def referer = r.getHeader("Referer")
}

object Step
{
  type Params = Map[String, String]
  type Action = Any => String
  
  val protocols = List("GET", "POST", "PUT", "DELETE")
}
import Step._

abstract class Step extends HttpServlet
{
  val Routes      = Map(protocols map (_ -> new HashSet[Route]): _*)
  val paramsMap   = new DynamicVariable[Params](null)
  var contentType = "text/html; charset=utf-8"
  var characterEncoding = "UTF-8"
  val _session    = new DynamicVariable[Session](null)
  val _response   = new DynamicVariable[HttpServletResponse](null)
  val _request    = new DynamicVariable[StepRequest](null)

  implicit def requestToStepRequest(r: HttpServletRequest) = StepRequest(r)
  
  class Route(val path: String, val action: Action) {
    val pattern = """:\w+"""
    val names = new Regex(pattern) findAllIn path toList
    val re = new Regex("^%s$" format path.replaceAll(pattern, "(.*?)"))      

    def apply(realPath: String): Option[Params] =
      re findFirstMatchIn realPath map (x => Map(names zip x.subgroups : _*))

    override def toString() = path
  }

  override def service(request: HttpServletRequest, response: HttpServletResponse) {
    // As default, the servlet tries to decode params with ISO_8859-1.
    // It causes an EOFException if params are actually encoded with the other code (such as UTF-8)
    request.setCharacterEncoding(characterEncoding)
    
    val realParams = new MapWrapper[String, Array[String]]() {
      def underlying = request.getParameterMap.asInstanceOf[java.util.Map[String,Array[String]]]
    }.map { case (k,v) => (k, v(0)) }
    
    def isMatchingRoute(route: Route) = {
      def exec(args: Params) = {
        before _
        response setContentType contentType
	_request.withValue(request) {
	  _response.withValue(response) {
	    _session.withValue(request) {
              paramsMap.withValue(args ++ realParams withDefaultValue(null)) {
		response.getWriter print route.action()
              }
	    }
	  }
	}
      } 
      //getPathInfo returns everything after the context path, so step will work if non-root
      route(request.getPathInfo) map exec isDefined
    }
    
    if (Routes(request.getMethod) find isMatchingRoute isEmpty)
      response.getWriter println "Requesting %s but only have %s".format(request.getRequestURI, Routes)
  }

  def before(fun: => Any) = fun
  def params = paramsMap value
  def redirect(uri: String) = (_response value) sendRedirect uri
  def request = _request value
  def session = _session value
  def status(code: Int) = (_response value) setStatus code

  val List(get, post, put, delete) = protocols map routeSetter

  // functional programming means never having to repeat yourself
  private def routeSetter(protocol: String): (String) => (=> Any) => Unit = {
    def g(path: String, fun: => Any) = Routes(protocol) += new Route(path, x => fun.toString)
    (g _).curry
  }  
}
