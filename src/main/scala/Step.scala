package com.thinkminimo.step

import javax.servlet._;
import javax.servlet.http._;
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.mutable.HashSet
import scala.collection.jcl.MapWrapper

abstract class Step extends HttpServlet {
  type Params = Map[String,String]
  type Action = Any=>String
  
  class Route(val path:String,val action:Action) {
    private val pattern = ":\\w+"  

    val names = new Regex(pattern).findAllIn(path).toList
    val re = new Regex("^"+path.replaceAll(pattern,"(.*?)")+"$")

    def apply(realPath: String): Option[Params]= 
      re.findFirstMatchIn(realPath) match {
        case Some(matches) => {
          Some(Map()++(names zip matches.subgroups))
        }
        case None => None 
      }
  }
    
  val Routes = Map[String,HashSet[Route]](
    "GET"->new HashSet[Route](), 
    "POST"->new HashSet[Route](),
    "PUT"->new HashSet[Route](), 
    "DELETE"->new HashSet[Route]()
  )

  val paramsMap = new DynamicVariable[Params](null)
  var contentType = "text/html"

  def params(name:String):String = paramsMap.value(name)

  override def service(request:HttpServletRequest, response: HttpServletResponse) {

        val realParams = new MapWrapper[String, Array[String]]() {
            def underlying = request.getParameterMap.asInstanceOf[java.util.Map[String,Array[String]]]
        }.map { case(k,v) => k->v(0)}  
       
        Routes(request.getMethod).find( route  =>
          route(request.getRequestURI) match {
            case Some(args) => {
              before()_
              response.setContentType(contentType)
              paramsMap.withValue(args ++ realParams){
                response.getWriter.println(route.action())
              }
              true
            }
            case None => false
         }) match {
          case None => response.getWriter.println("Requesting "+ request.getRequestURI+" but only have " + Routes)
          case Some(pair) => None
        }
        
    }
  
  def before()(fun: =>Any) = fun
  def get(path:String)(fun: =>Any) = Routes("GET") += new Route(path,x=>fun.toString)
  def post(path:String)(fun: =>Any) = Routes("POST") += new Route(path,x=>fun.toString)
  def put(path:String)(fun: =>Any) = Routes("PUT") += new Route(path,x=>fun.toString)
  def delete(path:String)(fun: =>Any) = Routes("DELETE") += new Route(path,x=>fun.toString)
  
}
