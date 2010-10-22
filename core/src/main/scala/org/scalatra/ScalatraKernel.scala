package org.scalatra

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.JavaConversions._
import scala.xml.NodeSeq
import collection.mutable.{ListBuffer, HashMap, Map => MMap}
import util.{MapWithIndifferentAccess, MultiMapHeadView}

object ScalatraKernel
{
  type MultiParams = Map[String, Seq[String]]
  
  type Action = () => Any

  val httpMethods = List("GET", "POST", "PUT", "DELETE")
  val writeMethods = "POST" :: "PUT" :: "DELETE" :: Nil
  val csrfKey = "csrfToken"

  val EnvironmentKey = "org.scalatra.environment"
}
import ScalatraKernel._


trait ScalatraKernel extends Handler with Initializable
{
  protected val Routes = MMap(httpMethods map (_ -> List[Route]()): _*)

  protected def contentType = response.getContentType
  protected def contentType_=(value: String): Unit = response.setContentType(value)

  protected val defaultCharacterEncoding = "UTF-8"
  private val _response   = new DynamicVariable[HttpServletResponse](null)
  private val _request    = new DynamicVariable[HttpServletRequest](null)

  protected implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)
  protected implicit def sessionWrapper(s: HttpSession) = new RichSession(s)
  protected implicit def servletContextWrapper(sc: ServletContext) = new RichServletContext(sc)

  protected[scalatra] class Route(val routeMatchers: Iterable[RouteMatcher], val action: Action) {
    def apply(realPath: String): Option[Any] = RouteMatcher.matchRoute(routeMatchers) flatMap { invokeAction(_) }

    private def invokeAction(routeParams: MultiParams) =
      _multiParams.withValue(multiParams ++ routeParams) {
        try {
          Some(action.apply())
        }
        catch {
          case e: PassException => None
        }
      }

    override def toString() = routeMatchers.toString
  }

  protected implicit def string2RouteMatcher(path: String): RouteMatcher = {
    val (re, names) = PathPatternParser.parseFrom(path)

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

  protected implicit def regex2RouteMatcher(regex: Regex): RouteMatcher = new RouteMatcher {
    def apply() = regex.findFirstMatchIn(requestPath) map { _.subgroups match {
      case Nil => Map.empty
      case xs => Map("captures" -> xs)
    }}
    
    override def toString = regex.toString
  }

  protected implicit def booleanBlock2RouteMatcher(matcher: => Boolean): RouteMatcher =
    () => { if (matcher) Some(Map[String, Seq[String]]()) else None }
  
  def handle(request: HttpServletRequest, response: HttpServletResponse) {
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
            case e => handleError(e)
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

  protected def handleError(e: Throwable): Any = {
    status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    _caughtThrowable.withValue(e) { errorHandler() }
  }
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
  protected val _params = new MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] {
    protected def multiMap = multiParams
  }
  protected def params = _params

  protected def redirect(uri: String) = (_response value) sendRedirect uri
  protected implicit def request = _request value
  protected implicit def response = _response value
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

  protected def get(routeMatchers: RouteMatcher*)(action: => Any) = addRoute("GET", routeMatchers, action)
  protected def post(routeMatchers: RouteMatcher*)(action: => Any) = addRoute("POST", routeMatchers, action)
  protected def put(routeMatchers: RouteMatcher*)(action: => Any) = addRoute("PUT", routeMatchers, action)
  protected def delete(routeMatchers: RouteMatcher*)(action: => Any) = addRoute("DELETE", routeMatchers, action)
  private def addRoute(protocol: String, routeMatchers: Iterable[RouteMatcher], action: => Any): Unit =
    Routes(protocol) = new Route(routeMatchers, () => action) :: Routes(protocol)

  private var config: Config = _
  def initialize(config: Config) = this.config = config

  protected def initParameter(name: String): Option[String] = config match {
    case config: ServletConfig => Option(config.getInitParameter(name))
    case config: FilterConfig => Option(config.getInitParameter(name))
    case _ => None
  }

  def environment: String = System.getProperty(EnvironmentKey, initParameter(EnvironmentKey).getOrElse("development"))
  def isDevelopmentMode = environment.toLowerCase.startsWith("dev")
}
