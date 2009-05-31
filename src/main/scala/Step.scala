package com.thinkminimo.step
 
import javax.servlet._;
import javax.servlet.http._;
import scala.util.DynamicVariable
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.collection.jcl.MapWrapper

abstract class Step extends HttpServlet {

  type reqParams = ArrayBuffer[(String,Array[String])]
  val routeMap = new HashMap[Route,(Any=>String)]()
  val paramsMap = new DynamicVariable[reqParams](null)
  var content_type = "text/html"

  //We do these crazy shenanigans because we're using MapWrapper, which runs on an ArrayBuffer
  def params(name:String):String = paramsMap.value.toList.filter(s => s._1 == name).head._2(0)

  class Route(method:String, path:String) {

    def reqMethod:String = method
    def getPath:String = path

    var parts = List[String]()
    var size = 0
    var placeholders = List[Int]()
    var constants = List[Int]()

    if(path != "/") {

      parts = path.split('/').toList.tail
      size = parts.length

      //Build lists of 'parts', storing the locations in the path each 'part' occupies.
      //This is used later for mapping parameters to pieces of the request URI.
      placeholders = parts.zipWithIndex.filter(s => s._1.startsWith(":")).map(s => s._2).sort((s,t) => s < t)
      constants = parts.zipWithIndex.filter(s => !s._1.startsWith(":")).map(s=>s._2).sort((s,t) => s < t)
    }

    def matchesRequest(method:String, reqpath:String):Boolean = {
      var pathSize = 0
      if(reqpath != "/") {
        pathSize = reqpath.split('/').toList.tail.length
      }
      method == reqMethod &&
      pathSize == size
    }

    def matchesConstants(reqpath:String):Boolean = {
      if(reqpath != "/") {
        return reqpath.split('/').toList.tail.zipWithIndex.filter(part => constants.exists(place => part._2 == place)).map(_._1) ==
        path.split('/').toList.tail.zipWithIndex.filter(part => constants.exists(place => part._2 == place)).map(_._1)
      } else {
        return true;
      }
    }

    def parseParams(path:String):Map[String,Array[String]] = {
      Map() ++ (for {
        pair <- path.split('/').toList.tail.zipWithIndex.filter(part => placeholders.exists(place => part._2 == place))
      } yield (parts(pair._2).substring(1) -> Array(pair._1)))
    }
  }

  def matchRoute(method:String, reqpath:String):(Route, (Any=>String)) = {
    routeMap.filter(_._1.matchesRequest(method, reqpath)).filter(_._1.matchesConstants(reqpath)).elements.toList.first
  }

  //a hack for java.util.Map compatibility
  def toMap[K, E](m: java.util.Map[K, E]) = new MapWrapper[K, E] { def underlying = m } 

  def getRoutes():String = {
    var routes = "Route not found. Available routes: \n\n"
    for(route <- routeMap) {
      routes += route._1.getPath+"\n"
    }
    return "<pre>"+routes+"</pre>";
  }

  override def service(request:HttpServletRequest, response: HttpServletResponse) {
      try {

        before()_
        response.setContentType(content_type)
        val matchedRoute = matchRoute(request.getMethod, request.getRequestURI)
        if(request.getRequestURI == "/") {
          val requestMap =  toMap(request.getParameterMap)
          response.getWriter.println(matchedRoute._2());
        } else {
          val requestMap =  toMap(request.getParameterMap) ++ matchedRoute._1.parseParams(request.getRequestURI) 
          paramsMap.withValue(requestMap.asInstanceOf[reqParams]) {
            println(matchedRoute._2())
            response.getWriter.println(matchedRoute._2());
          }
        }
      } catch {
         case ex:NoSuchElementException => { 
           response.getWriter.println(getRoutes);
         }
       }
    }  

  def before()(fun: =>Any) = fun
  def delete(path:String)(fun: =>Any) = routeMap.put(new Route("DELETE",path),x=>fun.toString)
  def get(path:String)(fun: =>Any) = routeMap.put(new Route("GET",path),x=>fun.toString)
  def post(path:String)(fun: =>Any) = routeMap.put(new Route("POST",path),x=>fun.toString)
  def put(path:String)(fun: =>Any) = routeMap.put(new Route("PUT",path),x=>fun.toString)
}
