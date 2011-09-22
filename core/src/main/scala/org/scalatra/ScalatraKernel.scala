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
 * ScalatraKernel is the default implementation of [[org.scalatra.CoreDSL]].
 * It is typically extended by [[org.scalatra.ScalatraServlet]] or
 * [[org.scalatra.ScalatraFilter]] to create a Scalatra application.
 */
trait ScalatraKernel extends Handler with CoreDsl with Initializable
  with ServletApiImplicits
{
  /**
   * The routes registered in this kernel.
   */
  protected lazy val routes: RouteRegistry = new RouteRegistry

  /**
   * The default character encoding for requests and responses.
   */
  protected val defaultCharacterEncoding = "UTF-8"

  /**
   * A dynamic variable containing the currently-scoped response.  Should
   * not typically be invoked directly.  Prefer `response`.
   *
   * @see #response
   */
  protected val _response   = new DynamicVariable[HttpServletResponse](null)

  /**
   * A dynamic variable containing the currently-scoped request.  Should
   * not typically be invoked directly.  Prefer `request`.
   *
   * @see #request
   */
  protected val _request    = new DynamicVariable[HttpServletRequest](null)

  /**
   * Pluggable way to convert a path expression to a route matcher.
   * The default implementation is compatible with Sinatra's route syntax.
   *
   * @param path a path expression
   * @return a route matcher based on `path`
   */
  protected implicit def string2RouteMatcher(path: String): RouteMatcher =
    new SinatraRouteMatcher(path, requestPath)

  /**
   * Path pattern is decoupled from requests.  This adapts the PathPattern to
   * a RouteMatcher by supplying the request path.
   */
  protected implicit def pathPatternParser2RouteMatcher(pattern: PathPattern): RouteMatcher =
    new PathPatternRouteMatcher(pattern, requestPath)

  /**
   * Converts a regular expression to a route matcher.
   *
   * @param regex the regular expression
   * @return a route matcher based on `regex`
   * @see [[org.scalatra.RegexRouteMatcher]]
   */
  protected implicit def regex2RouteMatcher(regex: Regex): RouteMatcher =
    new RegexRouteMatcher(regex, requestPath)

  /**
   * Converts a boolean expression to a route matcher.
   *
   * @param block a block that evaluates to a boolean
   *
   * @return a route matcher based on `block`.  The route matcher should
   * return `Some` if the block is true and `None` if the block is false.
   *
   * @see [[org.scalatra.BooleanBlockRouteMatcher]]
   */
  protected implicit def booleanBlock2RouteMatcher(block: => Boolean): RouteMatcher =
    new BooleanBlockRouteMatcher(block)

  /**
   * Handles a request and renders a response.
   *
   * $ 1. If the request lacks a character encoding, `defaultCharacterEncoding`
   *      is set to the request.
   *
   * $ 2. Sets the response's character encoding to `defaultCharacterEncoding`.
   *
   * $ 3. Binds the current `request`, `response`, and `multiParams`, and calls
   *      `executeRoutes()`.
   */
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
        request(MultiParamsKey) = MultiMap(Map() ++ realMultiParams)
        executeRoutes() // IPC: taken out because I needed the extension point
      }
    }
  }

  /**
   * Executes routes in the context of the current request and response.
   *
   * $ 1. Executes each before filter with `runFilters`.
   * $ 2. Executes the routes in the route registry with `runRoutes` for
   *      the request's method.
   *      a. The result of runRoutes becomes the _action result_.
   *      b. If no route matches the requested method, but matches are
   *         found for other methods, then the `doMethodNotAllowed` hook is
   *         run with each matching method.
   *      c. If no route matches any method, then the `doNotFound` hook is
   *         run, and its return value becomes the action result.
   * $ 3. If an exception is thrown during the before filters or the route
   * $    actions, then it is passed to the `errorHandler` function, and its
   * $    result becomes the action result.
   * $ 4. Executes the after filters with `runFilters`.
   * $ 5. The action result is passed to `renderResponse`.
   */
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

  /**
   * Invokes each filters with `invoke`.  The results of the filters
   * are discarded.
   */
  protected def runFilters(filters: Traversable[Route]) =
    for {
      route <- filters
      matchedRoute <- route()
    } invoke(matchedRoute)

  /**
   * Lazily invokes routes with `invoke`.  The results of the routes
   * are returned as a stream.
   */
  protected def runRoutes(routes: Traversable[Route]) =
    for {
      route <- routes.toStream // toStream makes it lazy so we stop after match
      matchedRoute <- route()
      actionResult <- invoke(matchedRoute)
    } yield actionResult

  /**
   * Invokes a route or filter.  The multiParams gathered from the route
   * matchers are merged into the existing route params, and then the action
   * is run.
   *
   * @param matchedRoute the matched route to execute
   *
   * @param return the result of the matched route's action wrapped in `Some`,
   * or `None` if the action calls `pass`.
   */
  protected def invoke(matchedRoute: MatchedRoute) =
    withRouteMultiParams(Some(matchedRoute)) {
      try {
        Some(matchedRoute.action())
      }
      catch {
        case e: PassException => None
      }
    }

  /**
   * The effective path against which routes are matched.  The definition
   * varies between servlets and filters.
   */
  def requestPath: String

  def before(routeMatchers: RouteMatcher*)(fun: => Any) =
    addBefore(routeMatchers, fun)

  private def addBefore(routeMatchers: Iterable[RouteMatcher], fun: => Any) =
    routes.appendBeforeFilter(Route(routeMatchers, () => fun))

  def after(routeMatchers: RouteMatcher*)(fun: => Any) =
    addAfter(routeMatchers, fun)

  private def addAfter(routeMatchers: Iterable[RouteMatcher], fun: => Any) =
    routes.appendAfterFilter(Route(routeMatchers, () => fun))

  /**
   * Called if no route matches the current request for any method.  The
   * default implementation varies between servlet and filter.
   */
  protected var doNotFound: Action
  def notFound(fun: => Any) = doNotFound = { () => fun }

  /**
   * Called if no route matches the current request method, but routes
   * match for other methods.  By default, sends an HTTP status of 405
   * and an `Allow` header containing a comma-delimited list of the allowed
   * methods.
   */
  protected var doMethodNotAllowed: (Set[HttpMethod] => Any) = { allow =>
    status = 405
    response.setHeader("Allow", allow.mkString(", "))
  }
  def methodNotAllowed(f: Set[HttpMethod] => Any) = doMethodNotAllowed = f

  private def matchOtherMethods(): Option[Any] = {
    val allow = routes.matchingMethodsExcept(request.method)
    if (allow.isEmpty) None else Some(doMethodNotAllowed(allow))
  }

  /**
   * The error handler function, called if an exception is thrown during
   * before filters or the routes.
   */
  protected var errorHandler: ErrorHandler = { case t => throw t }
  def error(handler: ErrorHandler) = errorHandler = handler orElse errorHandler

  protected def withRouteMultiParams[S](matchedRoute: Option[MatchedRoute])(thunk: => S): S = {
    val originalParams = multiParams
    request(MultiParamsKey) = originalParams ++ matchedRoute.map(_.multiParams).getOrElse(Map.empty)
    try { thunk } finally { request(MultiParamsKey) = originalParams }
  }

  /**
   * Renders the action result to the response.
   * $ - If the content type is still null, call the contentTypeInferrer.
   * $ - Call the render pipeline on the result.
   */
  protected def renderResponse(actionResult: Any) {
    if (contentType == null)
      contentTypeInferrer.lift(actionResult) foreach { contentType = _ }
    renderResponseBody(actionResult)
  }

  /**
   * A partial function to infer the content type from the action result.
   *
   * @return
   *   $ - "text/plain" for String
   *   $ - "application/octet-stream" for a byte array
   *   $ - "text/html" for any other result
   */
  protected def contentTypeInferrer: ContentTypeInferrer = {
    case _: String => "text/plain"
    case _: Array[Byte] => "application/octet-stream"
    case _ => "text/html"
  }

  /**
   * Renders the action result to the response body via the render pipeline.
   *
   * @see #renderPipeline
   */
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

  /**
   * The current multiparams.  Multiparams are a result of merging the
   * standard request params (query string or post params) with the route
   * parameters extracted from the route matchers of the current route.
   * The default value for an unknown param is the empty sequence.  Invalid
   * outside `handle`.
   */
  def multiParams: MultiParams = request(MultiParamsKey).asInstanceOf[MultiParams]
    .withDefaultValue(Seq.empty)

  /*
   * Assumes that there is never a null or empty value in multiParams.  The servlet container won't put them
   * in request.getParameters, and we shouldn't either.
   */
  protected val _params = new MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] {
    protected def multiMap = multiParams
  }

  /**
   * A view of `multiParams`.  Returns the head element for any known param,
   * and is undefined for any unknown param.  Invalid outside `handle`.
   */
  def params = _params

  /**
   * The currently scoped request.  Invalid outside `handle`.
   */
  implicit def request = _request value

  /**
   * The currently scoped response.  Invalid outside `handle`.
   */
  implicit def response = _response value

  /**
   * Immediately halts processing of a request.  Can be called from either a
   * before filter or a route.
   *
   * @param status the status to set on the response, or null to leave
   *        the status unchanged.
   * @param body a result to render through the render pipeline as the body
   * @param headers headers to add to the response
   * @param reason the HTTP status reason to set, or null to leave unchanged.
   */
  def halt[T : Manifest](status: JInteger = null,
           body: T = (),
           headers: Map[String, String] = Map.empty,
           reason: String = null): Nothing = {
    val statusOpt = if (status == null) None else Some(status.intValue)
    throw new HaltException(statusOpt, Some(reason), headers, body)
  }

  /**
   * Implementation detail.  Do not rely on this.
   */
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

  /**
   * Immediately exits from the current route.
   */
  def pass() = throw new PassException

  /**
   * Implementation detail.  Do not rely on this.
   */
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
   * $ - restricting protocols
   * $ - namespace routes based on class name
   * $ - raising errors on overlapping entries.
   *
   * This is the method invoked by get(), post() etc.
   *
   * @see org.scalatra.ScalatraKernel#removeRoute
   */
  protected def addRoute(method: HttpMethod, routeMatchers: Iterable[RouteMatcher], action: => Any): Route = {
    val route = Route(routeMatchers, () => action, () => routeBasePath)
    routes.prependRoute(method, route)
    route
  }

  /**
   * The base path for URL generation
   */
  protected def routeBasePath: String

  @deprecated("Use addRoute(HttpMethod, Iterable[RouteMatcher], =>Any)", "2.0")
  protected[scalatra] def addRoute(verb: String, routeMatchers: Iterable[RouteMatcher], action: => Any): Route =
    addRoute(HttpMethod(verb), routeMatchers, action)

  /**
   * Removes _all_ the actions of a given route for a given HTTP method.
   * If addRoute is overridden then this should probably be overriden too.
   *
   * @see org.scalatra.ScalatraKernel#addRoute
   */
  protected def removeRoute(method: HttpMethod, route: Route): Unit =
    routes.removeRoute(method, route)

  protected def removeRoute(method: String, route: Route): Unit =
    removeRoute(HttpMethod(method), route)

  /**
   * The configuration, typically a ServletConfig or FilterConfig.
   */
  private var config: Config = _

  /**
   * Initializes the kernel.  Used to provide context that is unavailable
   * when the instance is constructed, for example the servlet lifecycle.
   * Should set the `config` variable to the parameter.
   *
   * @param config the configuration.
   */
  def initialize(config: Config) = this.config = config

  /**
   * Gets an init paramter from the config if it is a ServletConfig or a
   * FilterConfig.
   *
   * @param name the name of the key
   *
   * @return an option containing the value of the parameter if defined, or
   * `None` if the parameter is not set or the config type has no concept of
   * init parameters.
   */
  def initParameter(name: String): Option[String] = config match {
    case config: ServletConfig => Option(config.getInitParameter(name))
    case config: FilterConfig => Option(config.getInitParameter(name))
    case _ => None
  }

  /**
   * The servlet context in which this kernel runs.
   */
  def servletContext: ServletContext

  /**
   * A free form string representing the environment.
   * `org.scalatra.Environment` is looked up as a system property, and if
   * absent, and init parameter.  The default value is `development`.
   */
  def environment: String = System.getProperty(EnvironmentKey, initParameter(EnvironmentKey).getOrElse("development"))

  /**
   * A boolean flag representing whether the kernel is in development mode.
   * The default is true if the `environment` begins with "dev", case
   * insensitve.
   */
  def isDevelopmentMode = environment.toLowerCase.startsWith("dev")
}
