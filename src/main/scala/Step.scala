package com.thinkminimo.step

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.mutable.HashSet
import scala.collection.jcl.MapWrapper

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
  var contentType = "text/html"
  
  class Route(val path: String, val action: Action) {
    val pattern = """:\w+"""
    val names = new Regex(pattern) findAllIn path toList
    val re = new Regex("^%s$" format path.replaceAll(pattern, "(.*?)"))      

    def apply(realPath: String): Option[Params] =
      re findFirstMatchIn realPath map (x => Map(names zip x.subgroups : _*))

    override def toString():String = path
  }

  override def service(request: HttpServletRequest, response: HttpServletResponse) {
    val realParams = new MapWrapper[String, Array[String]]() {
      def underlying = request.getParameterMap.asInstanceOf[java.util.Map[String,Array[String]]]
    }.map { case (k,v) => (k, v(0)) }
    
    def isMatchingRoute(route: Route) = {
      def exec(args: Params) = {
        before _
        response setContentType contentType
        paramsMap.withValue(args ++ realParams) {
          response.getWriter print route.action()
        }
      } 
      //getPathInfo returns everything after the context path, so step will work if non-root
      route(request.getPathInfo) map exec isDefined
    }
    
    if (Routes(request.getMethod) find isMatchingRoute isEmpty)
      response.getWriter println "Requesting %s but only have %s".format(request.getRequestURI, Routes)
  }
  
  def params(name: String): String = paramsMap value name
  def before(fun: => Any) = fun
  val List(get, post, put, delete) = protocols map routeSetter  

  // functional programming means never having to repeat yourself
  private def routeSetter(protocol: String): (String) => (=> Any) => Unit = {
    def g(path: String, fun: => Any) = Routes(protocol) += new Route(path, x => fun.toString)
    (g _).curry
  }  
}
