package org.scalatra

import scala.util.matching.Regex
import servlet.AsyncSupport
import util.io.zeroCopy
import java.io.{File, FileInputStream}
import java.net.URLEncoder.encode
import scala.annotation.tailrec
import util.{MultiMap, MapWithIndifferentAccess, MultiMapHeadView, using}
import rl.UrlCodingUtils
import java.nio.charset.Charset
import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

object UriDecoder {
  def firstStep(uri: String) = UrlCodingUtils.urlDecode(UrlCodingUtils.ensureUrlEncoding(uri), toSkip = PathPatternParser.PathReservedCharacters)
  def secondStep(uri: String) = uri.replaceAll("%23", "#").replaceAll("%2F", "/").replaceAll("%3F", "?")
}

object ScalatraBase {
  /**
   * A key for request attribute that contains any exception
   * that might have occured before the handling has been
   * propagated to ScalatraBase#handle (such as in
   * FileUploadSupport)
   */
  val PrehandleExceptionKey = "org.scalatra.PrehandleException"
}

/**
 * The base implementation of the Scalatra DSL.  Intended to be portable
 * to all supported backends.
 */
trait ScalatraBase extends CoreDsl with DynamicScope with Initializable
{
  /**
   * The routes registered in this kernel.
   */
  lazy val routes: RouteRegistry = new RouteRegistry

  /**
   * The default character encoding for requests and responses.
   */
  protected val defaultCharacterEncoding = "UTF-8"

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
  override def handle(request: HttpServletRequest, response: HttpServletResponse) {
//    val realMultiParams = request.multiParameters

    response.characterEncoding = Some(defaultCharacterEncoding)

    withRequestResponse(request, response) {
//      request(MultiParamsKey) = MultiMap(Map() ++ realMultiParams)
      executeRoutes()
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
  protected def executeRoutes() {
    var result: Any = null
    try {
      val prehandleException = request.get("org.scalatra.PrehandleException")
      if (prehandleException.isEmpty) {
        runFilters(routes.beforeFilters)
        val actionResult = runRoutes(routes(request.requestMethod)).headOption orElse
          matchOtherMethods() getOrElse doNotFound()
        // Give the status code handler a chance to override the actionResult
        result = handleStatusCode(status) getOrElse actionResult
      } else {
        throw prehandleException.get.asInstanceOf[Exception]
      }
    }
    catch {
      case e: HaltException => renderHaltException(e)
      case e => {
        try {
          result = errorHandler(e)
        } catch {
          case e2: HaltException => renderHaltException(e2)
        }
      }
    }
    finally {
      if (result == null || !isAsyncExecutable(result)) {
        runFilters(routes.afterFilters)
      }
    }

    renderResponse(result)
  }

  protected def isAsyncExecutable(result: Any) = false

  /**
   * Invokes each filters with `invoke`.  The results of the filters
   * are discarded.
   */
  protected def runFilters(filters: Traversable[Route]) {
    for {
      route <- filters
      matchedRoute <- route()
    } invoke(matchedRoute)
  }

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
   * @return the result of the matched route's action wrapped in `Some`,
   * or `None` if the action calls `pass`.
   */
  protected def invoke(matchedRoute: MatchedRoute) =
    withRouteMultiParams(Some(matchedRoute)) {
      liftAction(matchedRoute.action)
    }

  private def liftAction(action: Action): Option[Any] = 
    try {
      Some(action())
    }
    catch {
      case e: PassException => None
    }

  /**
   * The effective path against which routes are matched.  The definition
   * varies between servlets and filters.
   */
  def requestPath: String

  def before(transformers: RouteTransformer*)(fun: => Any) {
    routes.appendBeforeFilter(Route(transformers, () => fun))
  }

  def after(transformers: RouteTransformer*)(fun: => Any) {
    routes.appendAfterFilter(Route(transformers, () => fun))
  }

  /**
   * Called if no route matches the current request for any method.  The
   * default implementation varies between servlet and filter.
   */
  protected var doNotFound: Action
  def notFound(fun: => Any) { doNotFound = { () => fun } }

  /**
   * Called if no route matches the current request method, but routes
   * match for other methods.  By default, sends an HTTP status of 405
   * and an `Allow` header containing a comma-delimited list of the allowed
   * methods.
   */
  protected var doMethodNotAllowed: (Set[HttpMethod] => Any) = { allow =>
    status = 405
    response.headers("Allow") = allow.mkString(", ")
  }
  def methodNotAllowed(f: Set[HttpMethod] => Any) { doMethodNotAllowed = f }

  private def matchOtherMethods(): Option[Any] = {
    val allow = routes.matchingMethodsExcept(request.requestMethod)
    if (allow.isEmpty) None else liftAction(() => doMethodNotAllowed(allow))
  }

  private def handleStatusCode(status: Int): Option[Any] =
    for (handler <- routes(status);
	 matchedHandler <- handler();
         handlerResult <- invoke(matchedHandler)
    ) yield handlerResult

  /**
   * The error handler function, called if an exception is thrown during
   * before filters or the routes.
   */
  protected var errorHandler: ErrorHandler = { case t => throw t }
  def error(handler: ErrorHandler) { errorHandler = handler orElse errorHandler }

  protected def withRouteMultiParams[S](matchedRoute: Option[MatchedRoute])(thunk: => S): S = {
    val originalParams = multiParams
    val routeParams = matchedRoute.map(_.multiParams).getOrElse(Map.empty).map { case (key, values) =>
      key -> values.map(UriDecoder.secondStep(_))
    }
    request(MultiParamsKey) = originalParams ++ routeParams
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
    case actionResult: ActionResult =>
      actionResult.headers.find {
        case (name, value) => name.toLowerCase == "content-type"
      }.getOrElse(("content-type", contentTypeInferrer(actionResult.body)))._2

    case _ => "text/html"
  }

  /**
   * Renders the action result to the response body via the render pipeline.
   *
   * @see #renderPipeline
   */
  protected def renderResponseBody(actionResult: Any) {
    @tailrec def loop(ar: Any): Any = ar match {
      case _: Unit | Unit =>
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
    case status: Int =>
      response.status = ResponseStatus(status)
    case bytes: Array[Byte] =>
      response.outputStream.write(bytes)
    case is: java.io.InputStream => 
      using(is) { util.io.copy(_, response.outputStream) }
    case file: File =>
      using(new FileInputStream(file)) { in => zeroCopy(in, response.outputStream) }
    case _: Unit | Unit =>
      // If an action returns Unit, it assumes responsibility for the response
    case actionResult: ActionResult =>
      response.status = actionResult.status
      actionResult.headers.foreach { case(name, value) => response.addHeader(name, value) }
      actionResult.body
    case x: Any  =>
      response.writer.print(x.toString)
  }

  /**
   * The current multiparams.  Multiparams are a result of merging the
   * standard request params (query string or post params) with the route
   * parameters extracted from the route matchers of the current route.
   * The default value for an unknown param is the empty sequence.  Invalid
   * outside `handle`.
   */
  def multiParams: MultiParams = {
//    request(MultiParamsKey).asInstanceOf[MultiParams].withDefaultValue(Seq.empty)
    val read = request.contains("MultiParamsRead")
    val found = request.get(MultiParamsKey) map (
      _.asInstanceOf[MultiParams] ++ (if (read) Map.empty else request.multiParameters)
    )
    val multi = found getOrElse request.multiParameters
    request("MultiParamsRead") = new {}
    request(MultiParamsKey) = multi
    multi.withDefaultValue(Seq.empty)
  }

  /*
   * Assumes that there is never a null or empty value in multiParams.  The servlet container won't put them
   * in request.getParameters, and we shouldn't either.
   */
  protected val _params: MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] = new MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] {
    protected def multiMap = multiParams
  }

  /**
   * A view of `multiParams`.  Returns the head element for any known param,
   * and is undefined for any unknown param.  Invalid outside `handle`.
   */
  def params = _params

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

  protected def renderHaltException(e: HaltException) {
    e match {
      case HaltException(Some(status), Some(reason), _, _) =>
        response.status = ResponseStatus(status, reason)
      case HaltException(Some(status), None, _, _) =>
        response.status = ResponseStatus(status)
      case HaltException(None, _, _, _) => // leave status line alone
    }
    e.headers foreach { case(name, value) => response.addHeader(name, value) }
    renderResponse(e.body)
  }

  def get(transformers: RouteTransformer*)(action: => Any) = addRoute(Get, transformers, action)

  def post(transformers: RouteTransformer*)(action: => Any) = addRoute(Post, transformers, action)

  def put(transformers: RouteTransformer*)(action: => Any) = addRoute(Put, transformers, action)

  def delete(transformers: RouteTransformer*)(action: => Any) = addRoute(Delete, transformers, action)

  def trap(codes: Range)(block: => Any) { addStatusRoute(codes, block) }

  def options(transformers: RouteTransformer*)(action: => Any) = addRoute(Options, transformers, action)

  def patch(transformers: RouteTransformer*)(action: => Any) = addRoute(Patch, transformers, action)

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
  protected def addRoute(method: HttpMethod, transformers: Seq[RouteTransformer], action: => Any): Route = {
    val route = Route(transformers, () => action, () => routeBasePath)
    routes.prependRoute(method, route)
    route
  }

  /**
   * The base path for URL generation
   */
  protected def routeBasePath: String

  @deprecated("Use addRoute(HttpMethod, Seq[RouteMatcher], =>Any)", "2.0.0")
  protected[scalatra] def addRoute(verb: String, transformers: Seq[RouteTransformer], action: => Any): Route =
    addRoute(HttpMethod(verb), transformers, action)

  /**
   * Removes _all_ the actions of a given route for a given HTTP method.
   * If addRoute is overridden then this should probably be overriden too.
   *
   * @see org.scalatra.ScalatraKernel#addRoute
   */
  protected def removeRoute(method: HttpMethod, route: Route) {
    routes.removeRoute(method, route)
  }

  protected def removeRoute(method: String, route: Route) {
    removeRoute(HttpMethod(method), route)
  }

  protected[scalatra] def addStatusRoute(codes: Range, action: => Any)  {
    val route = Route(Seq.empty, () => action, () => routeBasePath)
    routes.addStatusRoute(codes, route)
  }

  /**
   * The configuration, typically a ServletConfig or FilterConfig.
   */
  private[this] var config: Config = _

  /**
   * Initializes the kernel.  Used to provide context that is unavailable
   * when the instance is constructed, for example the servlet lifecycle.
   * Should set the `config` variable to the parameter.
   *
   * @param config the configuration.
   */
  def initialize(config: ConfigT) { this.config = config }

  /**
   * Gets an init paramter from the config.
   *
   * @param name the name of the key
   *
   * @return an option containing the value of the parameter if defined, or
   * `None` if the parameter is not set.
   */
  def initParameter(name: String): Option[String] = 
    config.initParameters.get(name)

  /**
   * The servlet context in which this kernel runs.
   */
  def servletContext: ServletContext = config.context

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

  /**
   * Returns a context-relative, session-aware URL for a path and specified
   * parameters.
   * Finally, the result is run through `response.encodeURL` for a session
   * ID, if necessary.
   *
   * @param path the base path.  If a path begins with '/', then the context
   * path will be prepended to the result
   *
   * @param params params, to be appended in the form of a query string
   *
   * @return the path plus the query string, if any.  The path is run through
   * `response.encodeURL` to add any necessary session tracking parameters.
   */
  def url(path: String, params: Iterable[(String, Any)] = Iterable.empty): String = {
    val newPath = path match {
      case x if x.startsWith("/") => contextPath + path
      case _ => path
    }

    val pairs = params map {
      case (key, None) => encode(key, "utf-8")+"="
      case (key, Some(value)) => encode(key, "utf-8") + "=" +encode(value.toString, "utf-8")
      case(key, value) => encode(key, "utf-8") + "=" +encode(value.toString, "utf-8")
    }
    val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
    addSessionId(newPath+queryString)
  }

  protected def contextPath: String = servletContext.contextPath

  protected def addSessionId(uri: String): String
}
