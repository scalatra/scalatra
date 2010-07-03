package org.scalatra

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.JavaConversions._
import scala.xml.NodeSeq
import collection.mutable.{ListBuffer, HashMap, Map => MMap}

object ScalatraKernel
{
  type MultiParams = Map[String, Seq[String]]
  type RouteMatcher = () => Option[MultiParams]
  type Action = () => Any

  val protocols = List("GET", "POST", "PUT", "DELETE")
}
import ScalatraKernel._

trait ScalatraKernel
{
  protected val Routes = MMap(protocols map (_ -> List[Route]()): _*)

  protected def contentType = response.getContentType
  protected def contentType_=(value: String): Unit = response.setContentType(value)

  protected val defaultCharacterEncoding = "UTF-8"
  private val _response   = new DynamicVariable[HttpServletResponse](null)
  private val _request    = new DynamicVariable[HttpServletRequest](null)

  protected implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)
  protected implicit def sessionWrapper(s: HttpSession) = new RichSession(s)

  protected class Route(val routeMatcher: RouteMatcher, val action: Action) {
    def apply(realPath: String): Option[Any] = routeMatcher() flatMap { invokeAction(_) }

    private def invokeAction(routeParams: MultiParams) =
      _multiParams.withValue(multiParams ++ routeParams) {
        try {
          Some(action.apply())
        }
        catch {
          case e: PassException => None
        }
      }

    override def toString() = routeMatcher.toString
  }

  private def string2RouteMatcher(path: String): RouteMatcher = {
    // TODO put this out of its misery
    val (re, names) = {
      var names = new ListBuffer[String]
      var pos = 0
      var regex = new StringBuffer("^")
      val specialCharacters = List('.', '+', '(', ')')
      """:\w+|[\*\.\+\(\)\$]""".r.findAllIn(path).matchData foreach { md =>
        regex.append(path.substring(pos, md.start))
        md.toString match {
          case "*" =>
            names += ":splat"
            regex.append("(.*?)")
          case "." | "+" | "(" | ")" | "$" =>
            regex.append("\\").append(md.toString)
          case x =>
            names += x
            regex.append("([^/?]+)")
        }
        pos = md.end
      }
      regex.append(path.substring(pos))
      regex.append("$")
      (regex.toString.r, names.toList)
    }

    // By overriding toString, we can list the available routes in the default notFound handler.
    new RouteMatcher {
      def apply() = (re findFirstMatchIn requestPath)
        .map { reMatch => names zip reMatch.subgroups }
        .map { pairs =>
          val multiParams = new HashMap[String, ListBuffer[String]]
          pairs foreach { case (k, v) => if (v != null) multiParams.getOrElseUpdate(k, new ListBuffer) += v }
          Map() ++ multiParams
        }
      
      override def toString = path
    }
  }

  private def regex2RouteMatcher(regex: Regex): RouteMatcher = new RouteMatcher {
    def apply() = regex.findFirstMatchIn(requestPath) map { _.subgroups match {
      case Nil => Map.empty
      case xs => Map(":captures" -> xs)
    }}
    
    override def toString = regex.toString
  }

  private def booleanBlock2RouteMatcher(matcher: => Boolean): RouteMatcher =
    () => { if (matcher) Some(Map.empty) else None }
  
  protected def handle(request: HttpServletRequest, response: HttpServletResponse) {
    // As default, the servlet tries to decode params with ISO_8859-1.
    // It causes an EOFException if params are actually encoded with the other code (such as UTF-8)
    if (request.getCharacterEncoding == null)
      request.setCharacterEncoding(defaultCharacterEncoding)

    val realMultiParams = request.getParameterMap.asInstanceOf[java.util.Map[String,Array[String]]].toMap
      .transform { (k, v) => v: Seq[String] }

    response.setCharacterEncoding(defaultCharacterEncoding)

    _request.withValue(request) {
      _response.withValue(response) {
        _multiParams.withValue(Map() ++ realMultiParams) {
          val result = try {
            beforeFilters foreach { _() }
            Routes(request.getMethod).toStream.flatMap { _(requestPath) }.headOption.getOrElse(doNotFound())
          }
          catch {
            case HaltException(Some(code), Some(msg)) => response.sendError(code, msg)
            case HaltException(Some(code), None) => response.sendError(code)
            case HaltException(None, _) =>
            case e => _caughtThrowable.withValue(e) {
              status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
              errorHandler()
            }
          }
          finally {
            afterFilters foreach { _() }
          }
          renderResponse(result)
        }
      }
    }
  }
  
  protected def requestPath: String

  private val beforeFilters = new ListBuffer[() => Any]
  protected def before(fun: => Any) = beforeFilters += { () => fun }

  private val afterFilters = new ListBuffer[() => Any]
  protected def after(fun: => Any) = afterFilters += { () => fun }

  protected var doNotFound: Action
  protected def notFound(fun: => Any) = doNotFound = { () => fun }

  private var errorHandler: Action = { () => throw caughtThrowable }
  protected def error(fun: => Any) = errorHandler = { () => fun }
  private val _caughtThrowable = new DynamicVariable[Throwable](null)
  protected def caughtThrowable = _caughtThrowable.value

  protected def renderResponse(actionResult: Any) {
    if (contentType == null)
      contentType = inferContentType(actionResult)
    renderResponseBody(actionResult)
  }

  protected def inferContentType(actionResult: Any): String = actionResult match {
    case _: NodeSeq => "text/html"
    case _: Array[Byte] => "application/octet-stream"
    case _ => "text/plain"
  }

  protected def renderResponseBody(actionResult: Any) {
    actionResult match {
      case bytes: Array[Byte] =>
        response.getOutputStream.write(bytes)
      case _: Unit =>
      // If an action returns Unit, it assumes responsibility for the response
      case x: Any  =>
        response.getWriter.print(x.toString)
    }
  }

  private val _multiParams = new DynamicVariable[Map[String, Seq[String]]](Map())
  protected def multiParams: MultiParams = (_multiParams.value).withDefaultValue(Seq.empty)
  /*
   * Assumes that there is never a null or empty value in multiParams.  The servlet container won't put them
   * in request.getParameters, and we shouldn't either.
   */
  protected val _params = new collection.Map[String, String] {
    def get(key: String) = multiParams.get(key) flatMap { _.headOption }
    override def size = multiParams.size
    override def iterator = multiParams map { case(k, v) => (k, v.head) } iterator
    override def -(key: String) = Map() ++ this - key
    override def +[B1 >: String](kv: (String, B1)) = Map() ++ this + kv
  }
  protected def params = _params

  protected def redirect(uri: String) = (_response value) sendRedirect uri
  protected def request = _request value
  protected def response = _response value
  protected def session = request.getSession
  protected def sessionOption = request.getSession(false) match {
    case s: HttpSession => Some(s)
    case null => None
  }
  protected def status(code: Int) = (_response value) setStatus code

  protected def halt(code: Int, msg: String) = throw new HaltException(Some(code), Some(msg))
  protected def halt(code: Int) = throw new HaltException(Some(code), None)
  protected def halt() = throw new HaltException(None, None)
  private case class HaltException(val code: Option[Int], val msg: Option[String]) extends RuntimeException

  protected def pass() = throw new PassException
  private class PassException extends RuntimeException

  protected def get(path: String)(action: => Any) = addPathRoute("GET", path, action)
  protected def get(regex: Regex)(action: => Any) = addRegexRoute("GET", regex, action)
  protected def get(condition: => Boolean)(action: => Any) = addConditionRoute("GET", condition, action)
  protected def get(path: String, condition: => Boolean)(action: => Any) =
    addPathAndConditionRoute("GET", path, condition, action)
  protected def get(regex: Regex, condition: => Boolean)(action: => Any) =
    addRegexAndConditionRoute("GET", regex, condition, action)

  protected def post(path: String)(action: => Any) = addPathRoute("POST", path, action)
  protected def post(regex: Regex)(action: => Any) = addRegexRoute("POST", regex, action)
  protected def post(condition: => Boolean)(action: => Any) = addConditionRoute("POST", condition, action)
  protected def post(path: String, condition: => Boolean)(action: => Any) =
    addPathAndConditionRoute("POST", path, condition, action)
  protected def post(regex: Regex, condition: => Boolean)(action: => Any) =
    addRegexAndConditionRoute("POST", regex, condition, action)

  protected def put(path: String)(action: => Any) = addPathRoute("PUT", path, action)
  protected def put(regex: Regex)(action: => Any) = addRegexRoute("PUT", regex, action)
  protected def put(condition: => Boolean)(action: => Any) = addConditionRoute("PUT", condition, action)
  protected def put(path: String, condition: => Boolean)(action: => Any) =
    addPathAndConditionRoute("PUT", path, condition, action)
  protected def put(regex: Regex, condition: => Boolean)(action: => Any) =
    addRegexAndConditionRoute("PUT", regex, condition, action)

  protected def delete(path: String)(action: => Any) = addPathRoute("DELETE", path, action)
  protected def delete(regex: Regex)(action: => Any) = addRegexRoute("DELETE", regex, action)
  protected def delete(condition: => Boolean)(action: => Any) = addConditionRoute("DELETE", condition, action)
  protected def delete(path: String, condition: => Boolean)(action: => Any) =
    addPathAndConditionRoute("DELETE", path, condition, action)
  protected def delete(regex: Regex, condition: => Boolean)(action: => Any) =
    addRegexAndConditionRoute("DELETE", regex, condition, action)

  private def addPathRoute(protocol: String, path: String, action: => Any): Unit =
    addRoute(protocol, string2RouteMatcher(path), action)

  private def addRegexRoute(protocol: String, regex: Regex, action: => Any): Unit =
    addRoute(protocol, regex2RouteMatcher(regex), action)

  private def addConditionRoute(protocol: String, condition: => Boolean, action: => Any): Unit =
    addRoute(protocol, booleanBlock2RouteMatcher(condition), action)

  private def addPathAndConditionRoute(protocol: String, path: String, condition: => Boolean, action: => Any): Unit = {
    val routeMatcher = () => if (condition) string2RouteMatcher(path).apply() else None
    addRoute(protocol, routeMatcher, action)
  }

  private def addRegexAndConditionRoute(protocol: String, regex: Regex, condition: => Boolean, action: => Any): Unit = {
    val routeMatcher = () => if (condition) regex2RouteMatcher(regex).apply() else None
    addRoute(protocol, routeMatcher, action)
  }

  private def addRoute(protocol: String, routeMatcher: RouteMatcher, action: => Any): Unit =
    Routes(protocol) = new Route(routeMatcher, () => action) :: Routes(protocol) 
}
