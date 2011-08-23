package org.scalatra

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.JavaConversions._
import scala.collection.mutable.{ConcurrentMap, HashMap, ListBuffer, SynchronizedBuffer}
import scala.xml.NodeSeq
import util.io.zeroCopy
import java.io.{File, FileInputStream}
import java.lang.{Integer => JInteger}
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.tailrec
import util.{MultiMap, MapWithIndifferentAccess, MultiMapHeadView, using}

object ScalatraKernel
{
  type MultiParams = MultiMap

  type Action = () => Any

  @deprecated("Use HttpMethods.methods", "2.0")
  val httpMethods = HttpMethod.methods map { _.toString }

  @deprecated("Use HttpMethods.methods filter { !_.isSafe }", "2.0")
  val writeMethods = HttpMethod.methods filter { !_.isSafe } map { _.toString }

  @deprecated("Use CsrfTokenSupport.DefaultKey", "2.0")
  val csrfKey = CsrfTokenSupport.DefaultKey

  val EnvironmentKey = "org.scalatra.environment".intern

  val MultiParamsKey = "org.scalatra.MultiParams".intern
}
import ScalatraKernel._

/**
 * ScalatraKernel provides the DSL for building Scalatra applications.
 *
 * At it's core a type mixing in ScalatraKernel is a registry of possible actions,
 * every request is dispatched to the first route matching.
 *
 * The [[org.scalatra.ScalatraKernel#get]], [[org.scalatra.ScalatraKernel#post]],
 * [[org.scalatra.ScalatraKernel#put]] and [[org.scalatra.ScalatraKernel#delete]]
 * methods register a new action to a route for a given HTTP method, possibly
 * overwriting a previous one. This trait is thread safe.
 */
trait ScalatraKernel extends Handler with CoreDsl with Initializable
{
  protected lazy val routes: RouteRegistry = new RouteRegistry

  protected val defaultCharacterEncoding = "UTF-8"
  protected val _response   = new DynamicVariable[HttpServletResponse](null)
  protected val _request    = new DynamicVariable[HttpServletRequest](null)

  protected implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)
  protected implicit def sessionWrapper(s: HttpSession) = new RichSession(s)
  protected implicit def servletContextWrapper(sc: ServletContext) = new RichServletContext(sc)

  /**
   * Pluggable way to convert Strings into RouteMatchers.  By default, we
   * interpret them the same way Sinatra does.
   */
  protected implicit def string2RouteMatcher(path: String): RouteMatcher =
    new SinatraRouteMatcher(path, requestPath)

  /**
   * Path pattern is decoupled from requests.  This adapts the PathPattern to
   * a RouteMatcher by supplying the request path.
   */
  protected implicit def pathPatternParser2RouteMatcher(pattern: PathPattern): RouteMatcher =
    new PathPatternRouteMatcher(pattern, requestPath)

  protected implicit def regex2RouteMatcher(regex: Regex): RouteMatcher =
    new RegexRouteMatcher(regex, requestPath)

  protected implicit def booleanBlock2RouteMatcher(block: => Boolean): RouteMatcher =
    new BooleanBlockRouteMatcher(block)

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
        request(MultiParamsKey) = MultiMap(Map().withDefaultValue(Seq.empty[String]) ++ realMultiParams)
        executeRoutes() // IPC: taken out because I needed the extension point
      }
    }
  }

  protected def executeRoutes() = {
    val result = try {
      runFilters(routes.beforeFilters)
      val actionResult = runRoutes(routes(request.method)).headOption
      actionResult orElse matchOtherMethods() getOrElse doNotFound()
    }
    catch {
      case e: HaltException => renderHaltException(e)
      case e => errorHandler(e)
    }
    finally {
      runFilters(routes.afterFilters)
    }
    renderResponse(result)
  }

  protected def runFilters(filters: Traversable[Route]) =
    for {
      route <- filters
      matchedRoute <- route()
    } invoke(matchedRoute)

  protected def runRoutes(routes: Traversable[Route]) =
    for {
      route <- routes.toStream // toStream makes it lazy so we stop after match
      matchedRoute <- route()
      actionResult <- invoke(matchedRoute)
    } yield actionResult

  protected def invoke(matchedRoute: MatchedRoute) =
    withRouteMultiParams(Some(matchedRoute)) {
      try {
        Some(matchedRoute.action())
      }
      catch {
        case e: PassException => None
      }
    }

  def requestPath: String

  def before(routeMatchers: RouteMatcher*)(fun: => Any) =
    addBefore(routeMatchers, fun)

  private def addBefore(routeMatchers: Iterable[RouteMatcher], fun: => Any) =
    routes.appendBeforeFilter(Route(routeMatchers, () => fun))

  def after(routeMatchers: RouteMatcher*)(fun: => Any) =
    addAfter(routeMatchers, fun)

  private def addAfter(routeMatchers: Iterable[RouteMatcher], fun: => Any) =
    routes.appendAfterFilter(Route(routeMatchers, () => fun))

  protected var doNotFound: Action
  def notFound(fun: => Any) = doNotFound = { () => fun }

  protected var doMethodNotAllowed: (Set[HttpMethod] => Any) = { allow =>
    status(405)
    response.setHeader("Allow", allow.mkString(", "))
  }
  def methodNotAllowed(f: Set[HttpMethod] => Any) = doMethodNotAllowed = f

  private def matchOtherMethods(): Option[Any] = {
    val allow = routes.matchingMethodsExcept(request.method)
    if (allow.isEmpty) None else Some(doMethodNotAllowed(allow))
  }

  protected var errorHandler: ErrorHandler = { case t => throw t }
  def error(handler: ErrorHandler) = errorHandler = handler orElse errorHandler

  protected def withRouteMultiParams[S](matchedRoute: Option[MatchedRoute])(thunk: => S): S = {
    val originalParams = multiParams
    request(MultiParamsKey) = originalParams ++ matchedRoute.map(_.multiParams).getOrElse(Map.empty)
    try { thunk } finally { request(MultiParamsKey) = originalParams }
  }


  protected def renderResponse(actionResult: Any) {
    if (contentType == null)
      contentTypeInferrer.lift(actionResult) foreach { contentType = _ }
    renderResponseBody(actionResult)
  }

  protected def contentTypeInferrer: ContentTypeInferrer = {
    case _: String => "text/plain"
    case _: Array[Byte] => "application/octet-stream"
    case _ => "text/html"
  }

  protected def renderResponseBody(actionResult: Any) {
    @tailrec def loop(ar: Any): Any = ar match {
      case r: Unit =>
      case a => loop(renderPipeline.lift(a) getOrElse ())
    }
    loop(actionResult)
  }

  /**
   * The render pipeline is a partial function of Any => Any.  It is
   * called recursively until it returns ().  () indicates that the
   * response has been rendered.
   */
  protected def renderPipeline: RenderPipeline = {
    case bytes: Array[Byte] =>
      response.getOutputStream.write(bytes)
    case file: File =>
      using(new FileInputStream(file)) { in => zeroCopy(in, response.getOutputStream) }
    case _: Unit =>
      // If an action returns Unit, it assumes responsibility for the response
    case x: Any  =>
      response.getWriter.print(x.toString)
  }

  def multiParams: MultiParams = request(MultiParamsKey).asInstanceOf[MultiParams]
  /*
   * Assumes that there is never a null or empty value in multiParams.  The servlet container won't put them
   * in request.getParameters, and we shouldn't either.
   */
  protected val _params = new MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] {
    protected def multiMap = multiParams
  }
  def params = _params

  implicit def request = _request value
  implicit def response = _response value

  def halt[T : Manifest](status: JInteger = null,
           body: T = (),
           headers: Map[String, String] = Map.empty,
           reason: String = null): Nothing = {
    val statusOpt = if (status == null) None else Some(status.intValue)
    throw new HaltException(statusOpt, Some(reason), headers, body)
  }

  protected case class HaltException(
      status: Option[Int],
      reason: Option[String],
      headers: Map[String, String],
      body: Any)
   extends RuntimeException

  private def renderHaltException(e: HaltException) {
    e match {
      case HaltException(Some(status), Some(reason), _, _) => response.setStatus(status, reason)
      case HaltException(Some(status), None, _, _) => response.setStatus(status)
      case HaltException(None, _, _, _) => // leave status line alone
    }
    e.headers foreach { case(name, value) => response.addHeader(name, value) }
    renderResponse(e.body)
  }

  def pass() = throw new PassException
  protected[scalatra] class PassException extends RuntimeException

  def get(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Get, routeMatchers, action)

  def post(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Post, routeMatchers, action)

  def put(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Put, routeMatchers, action)

  def delete(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Delete, routeMatchers, action)

  /**
   * @see [[org.scalatra.ScalatraKernel.get]]
   */
  def options(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Options, routeMatchers, action)

  /**
   * @see [[org.scalatra.ScalatraKernel.get]]
   */
  def patch(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Patch, routeMatchers, action)

  /**
   * Prepends a new route for the given HTTP method.
   *
   * Can be overriden so that subtraits can use their own logic.
   * Possible examples:
   * - restricting protocols
   * - namespace routes based on class name
   * - raising errors on overlapping entries.
   *
   * This is the method invoked by get(), post() etc.
   *
   * @see org.scalatra.ScalatraKernel.removeRoute
   */
  protected def addRoute(method: HttpMethod, routeMatchers: Iterable[RouteMatcher], action: => Any): Route = {
    val route = Route(routeMatchers, () => action, () => request.getServletPath)
    routes.prependRoute(method, route)
    route
  }

  @deprecated("Use addRoute(HttpMethod, Iterable[RouteMatcher], =>Any)", "2.0")
  protected[scalatra] def addRoute(verb: String, routeMatchers: Iterable[RouteMatcher], action: => Any): Route =
    addRoute(HttpMethod(verb), routeMatchers, action)

  /**
   * removes _all_ the actions of a given route for a given HTTP method.
   * If [[addRoute]] is overriden this should probably be overriden too.
   *
   * @see org.scalatra.ScalatraKernel.addRoute
   */
  protected def removeRoute(method: HttpMethod, route: Route): Unit =
    routes.removeRoute(method, route)

  protected def removeRoute(method: String, route: Route): Unit =
    removeRoute(HttpMethod(method), route)

  private var config: Config = _
  def initialize(config: Config) = this.config = config

  def initParameter(name: String): Option[String] = config match {
    case config: ServletConfig => Option(config.getInitParameter(name))
    case config: FilterConfig => Option(config.getInitParameter(name))
    case _ => None
  }

  def servletContext: ServletContext

  def environment: String = System.getProperty(EnvironmentKey, initParameter(EnvironmentKey).getOrElse("development"))
  def isDevelopmentMode = environment.toLowerCase.startsWith("dev")
}
