package org.scalatra

import scala.util.matching.Regex
import servlet.ServletApiImplicits
import util.conversion.DefaultImplicitConversions
import util.io.zeroCopy
import java.io.{File, FileInputStream}
import scala.annotation.tailrec
import util._
import util.RicherString._
import rl.UrlCodingUtils
import javax.servlet.{Filter, ServletContext}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import scala.util.control.Exception._
import org.scalatra.ScalatraBase._
import scala.util.{Failure, Try, Success}
import scala.util.control.NonFatal

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
  val HostNameKey = "org.scalatra.HostName"
  val PortKey = "org.scalatra.Port"
  val ForceHttpsKey = "org.scalatra.ForceHttps"

  private[this] val KeyPrefix = classOf[FutureSupport].getName
  val Callbacks = s"$KeyPrefix.callbacks"
  val RenderCallbacks = s"$KeyPrefix.renderCallbacks"
  val IsAsyncKey = s"$KeyPrefix.isAsync"

  import servlet.ServletApiImplicits._
  def isAsyncResponse(implicit request: HttpServletRequest) = request.get(IsAsyncKey).getOrElse(false)

  def onSuccess(request: HttpServletRequest, fn: Any => Unit) = addCallback(request, _.foreach(fn))
  def onFailure(request: HttpServletRequest, fn: Throwable => Unit) = addCallback(request, _.failed.foreach(fn))
  def onCompleted(request: HttpServletRequest, fn: Try[Any] => Unit) = addCallback(request, fn)
  def onRenderedSuccess(request: HttpServletRequest, fn: Any => Unit) = addRenderCallback(request, _.foreach(fn))
  def onRenderedFailure(request: HttpServletRequest, fn: Throwable => Unit) = addRenderCallback(request, _.failed.foreach(fn))
  def onRenderedCompleted(request: HttpServletRequest, fn: Try[Any] => Unit) = addRenderCallback(request, fn)

  def callbacks(request: HttpServletRequest) =
    request.getOrElse(Callbacks, List.empty[Try[Any] => Unit]).asInstanceOf[List[Try[Any] => Unit]]

  def addCallback(request: HttpServletRequest, callback: Try[Any] => Unit) {
    request(Callbacks) = callback :: callbacks(request)
  }

  def runCallbacks(request: HttpServletRequest, data: Try[Any]) = callbacks(request).reverse foreach (_(data))
  def renderCallbacks(request: HttpServletRequest) =
    request.getOrElse(RenderCallbacks, List.empty[Try[Any] => Unit]).asInstanceOf[List[Try[Any] => Unit]]

  def addRenderCallback(request: HttpServletRequest, callback: Try[Any] => Unit) {
    request(RenderCallbacks) = callback :: renderCallbacks(request)
  }

  def runRenderCallbacks(request: HttpServletRequest, data: Try[Any]) = renderCallbacks(request).reverse foreach (_(data))

  import collection.JavaConverters._

  def getServletRegistration(app: ScalatraBase) = {
    val registrations = app.servletContext.getServletRegistrations.values().asScala.toList
    registrations.find(_.getClassName == app.getClass.getName)
  }

}

/**
 * The base implementation of the Scalatra DSL.  Intended to be portable
 * to all supported backends.
 */
trait ScalatraBase extends ScalatraContext with Initializable with ServletApiImplicits with ScalatraParamsImplicits with DefaultImplicitConversions with SessionSupport with Handler with Control {
  @deprecated("Use servletContext instead", "2.1.0")
  def applicationContext: ServletContext = servletContext

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
   * is set to the request.
   *
   * $ 2. Sets the response's character encoding to `defaultCharacterEncoding`.
   *
   * $ 3. Binds the current `request`, `response`, and `multiParams`, and calls
   * `executeRoutes()`.
   */
  override def handle(request: HttpServletRequest, response: HttpServletResponse) {
    //    val realMultiParams = request.multiParameters
    request(CookieSupport.SweetCookiesKey) = new SweetCookies(request.cookies, response)
    response.characterEncoding = Some(defaultCharacterEncoding)

      //      request(MultiParamsKey) = MultiMap(Map() ++ realMultiParams)
    executeRoutes(request, response)
  }


  /**
   * The servlet context in which this kernel runs.
   */
  def servletContext: ServletContext = config.context

  /**
   * Executes routes in the context of the current request and response.
   *
   * $ 1. Executes each before filter with `runFilters`.
   * $ 2. Executes the routes in the route registry with `runRoutes` for
   * the request's method.
   * a. The result of runRoutes becomes the _action result_.
   * b. If no route matches the requested method, but matches are
   * found for other methods, then the `doMethodNotAllowed` hook is
   * run with each matching method.
   * c. If no route matches any method, then the `doNotFound` hook is
   * run, and its return value becomes the action result.
   * $ 3. If an exception is thrown during the before filters or the route
   * $    actions, then it is passed to the `errorHandler` function, and its
   * $    result becomes the action result.
   * $ 4. Executes the after filters with `runFilters`.
   * $ 5. The action result is passed to `renderResponse`.
   */
  protected def executeRoutes(req: HttpServletRequest, resp: HttpServletResponse) {
    var result: Any = null
    var rendered = true

    def runActions = {
      val prehandleException = req.get(PrehandleExceptionKey)
      if (prehandleException.isEmpty) {
        onCompleted(req, { _ =>
          this match {
            case f: Filter if !req.contains("org.scalatra.ScalatraFilter.afterFilters.Run") =>
              req("org.scalatra.ScalatraFilter.afterFilters.Run") = new {}
              runFilters(req, resp, routes.afterFilters)
            case f: HttpServlet if !req.contains("org.scalatra.ScalatraServlet.afterFilters.Run") =>
              req("org.scalatra.ScalatraServlet.afterFilters.Run") = new {}
              runFilters(req, resp, routes.afterFilters)
            case _ =>
          }
        })
        runFilters(req, resp, routes.beforeFilters)
        val actionResult = runRoutes(req, resp, routes(req.requestMethod)).headOption
        // Give the status code handler a chance to override the actionResult
        val r = handleStatusCode(req, resp, status(resp)) getOrElse {
          actionResult orElse matchOtherMethods(req, resp) getOrElse doNotFound(req, resp)
        }
        rendered = false
        r
      } else {
        throw prehandleException.get.asInstanceOf[Exception]
      }
    }

    cradleHalt(req, resp, result = runActions, e => {
      cradleHalt(req, resp, {
        result = errorHandler(req, resp)(e)
        rendered = false
      }, e => {
        runCallbacks(req, Failure(e))
        try {
          renderUncaughtException(req, resp, e)
        } finally {
          runRenderCallbacks(req, Failure(e))
        }
      })
    })

    if (!rendered) renderResponse(req, resp, result)
  }

  private[this] def cradleHalt(req: HttpServletRequest,
                               resp: HttpServletResponse,
                               body: => Any,
                               error: Throwable => Any): Any = {
    try { body } catch {
      case e: HaltException => {
        try {
          handleStatusCode(req, resp, extractStatusCode(resp, e)) match {
            case Some(result) => renderResponse(req, resp, result)
            case _            => renderHaltException(req, resp, e)
          }
        } catch {
          case e: HaltException => renderHaltException(req, resp, e)
          case e: Throwable     => error(e)
        }
      }
      
      case e: Throwable => error(e)
    }
  }

  protected def renderUncaughtException(req: HttpServletRequest, resp: HttpServletResponse, e: Throwable) {
    implicit val rs = resp
    status = 500
    if (isDevelopmentMode) {
      contentType = "text/plain"
      e.printStackTrace(resp.getWriter)
    }
  }

  protected def isAsyncExecutable(result: Any) = false

  /**
   * Invokes each filters with `invoke`.  The results of the filters
   * are discarded.
   */
  protected def runFilters(req: HttpServletRequest, resp: HttpServletResponse, filters: Traversable[Route]) {
    for {
      route <- filters
      matchedRoute <- route(requestPath(req), req, resp)
    } invoke(req, resp, matchedRoute)
  }

  /**
   * Lazily invokes routes with `invoke`.  The results of the routes
   * are returned as a stream.
   */
  protected def runRoutes(req: HttpServletRequest, resp: HttpServletResponse, routes: Traversable[Route]) = {
    for {
      route <- routes.toStream // toStream makes it lazy so we stop after match
      matchedRoute <- route.apply(requestPath(req), req, resp)
      saved = saveMatchedRoute(req, matchedRoute)
      actionResult <- invoke(req, resp, saved)
    } yield actionResult
  }

  private[scalatra] def saveMatchedRoute(request: HttpServletRequest, matchedRoute: MatchedRoute) = {
    request("org.scalatra.MatchedRoute") = matchedRoute
    setMultiparams(request, Some(matchedRoute), multiParams(request))
    matchedRoute
  }

  private[scalatra] def matchedRoute(implicit request: HttpServletRequest) =
    request.get("org.scalatra.MatchedRoute").map(_.asInstanceOf[MatchedRoute])

  /**
   * Invokes a route or filter.  The multiParams gathered from the route
   * matchers are merged into the existing route params, and then the action
   * is run.
   *
   * @param matchedRoute the matched route to execute
   *
   * @return the result of the matched route's action wrapped in `Some`,
   *         or `None` if the action calls `pass`.
   */
  protected def invoke(req: HttpServletRequest, resp: HttpServletResponse, matchedRoute: MatchedRoute) =
    withRouteMultiParams(req, Some(matchedRoute)) {
      liftAction(req, resp, matchedRoute.action)
    }

  private def liftAction(req: HttpServletRequest, resp: HttpServletResponse, action: Action): Option[Any] =
    try {
      Some(action(req, resp))
    }
    catch {
      case e: PassException => None
    }


  def beforeAction(transformers: RouteTransformer*)(fun: Action) {
    routes.appendBeforeFilter(Route(transformers, fun))
  }

  def afterAction(transformers: RouteTransformer*)(fun: Action) {
    routes.appendAfterFilter(Route(transformers, fun))
  }

  /**
   * Called if no route matches the current request for any method.  The
   * default implementation varies between servlet and filter.
   */
  protected var doNotFound: Action

  def notFoundAction(fun: Action) { doNotFound = fun }

  /**
   * Called if no route matches the current request method, but routes
   * match for other methods.  By default, sends an HTTP status of 405
   * and an `Allow` header containing a comma-delimited list of the allowed
   * methods.
   */
  protected var doMethodNotAllowed: ((HttpServletRequest, HttpServletResponse, Set[HttpMethod]) => Any) = { (_, resp, allow) =>
    implicit val rs = resp
    status = 405
    resp.headers("Allow") = allow.mkString(", ")
  }

  def methodNotAllowed(f: (HttpServletRequest, HttpServletResponse, Set[HttpMethod]) => Any) {
    doMethodNotAllowed = f
  }

  private[this] def matchOtherMethods(req: HttpServletRequest, resp: HttpServletResponse): Option[Any] = {
    val allow = routes.matchingMethodsExcept(req.requestMethod, requestPath(req), req, resp)
    if (allow.isEmpty) None else liftAction(req, resp, {(req: HttpServletRequest, response: HttpServletResponse) =>
      doMethodNotAllowed(req, resp, allow)
    })
  }

  private[this] def handleStatusCode(req: HttpServletRequest, resp: HttpServletResponse, status: Int): Option[Any] =
    for {
      handler <- routes(status)
      matchedHandler <- handler(requestPath(req), req, resp)
      handlerResult <- invoke(req, resp, matchedHandler)
    } yield handlerResult

  /**
   * The error handler function, called if an exception is thrown during
   * before filters or the routes.
   */
  protected var errorHandler: ErrorHandlerAction = (_, _) => {
    case t => throw t
  }

  def errorAction(handler: ErrorHandlerAction) {
    errorHandler = {
      val old = errorHandler   // Need to do this to avoid making a recursive call and thus a stack overflow
      (req, resp) => handler(req, resp) orElse old(req, resp)
    }
  }

  protected def withRouteMultiParams[S](req: HttpServletRequest, matchedRoute: Option[MatchedRoute])(thunk: => S): S = {
    val originalParams = multiParams(req)
    setMultiparams(req, matchedRoute, originalParams)
    try {
      thunk
    } finally {
      req(MultiParamsKey) = originalParams
    }
  }

  protected def setMultiparams[S](request: HttpServletRequest, matchedRoute: Option[MatchedRoute], originalParams: MultiParams) {
    val routeParams = matchedRoute.map(_.multiParams).getOrElse(Map.empty).map {
      case (key, values) =>
        key -> values.map(s => if (s.nonBlank) UriDecoder.secondStep(s) else s)
    }
    request(MultiParamsKey) = originalParams ++ routeParams
  }

  /**
   * Renders the action result to the response.
   * $ - If the content type is still null, call the contentTypeInferrer.
   * $ - Call the render pipeline on the result.
   */
  protected def renderResponse(req: HttpServletRequest, resp: HttpServletResponse, actionResult: Any) {
    if (contentType(resp) == null)
      contentTypeInferrer(req, resp).lift(actionResult) foreach {
        contentType_=(_)(resp)
      }

    renderResponseBody(req, resp, actionResult)
  }

  /**
   * A partial function to infer the content type from the action result.
   *
   * @return
   * $ - "text/plain" for String
   * $ - "application/octet-stream" for a byte array
   * $ - "text/html" for any other result
   */
  protected def contentTypeInferrer(implicit req: HttpServletRequest, resp: HttpServletResponse): ContentTypeInferrer = {
    case s: String => "text/plain"
    case bytes: Array[Byte] => MimeTypes(bytes)
    case is: java.io.InputStream => MimeTypes(is)
    case file: File => MimeTypes(file)
    case actionResult: ActionResult =>
      actionResult.headers.find {
        case (name, value) => name equalsIgnoreCase "CONTENT-TYPE"
      }.getOrElse(("Content-Type", contentTypeInferrer(req, resp)(actionResult.body)))._2

    case _ => "text/html"
  }

  /**
   * Renders the action result to the response body via the render pipeline.
   *
   * @see #renderPipeline
   */
  protected def renderResponseBody(req: HttpServletRequest, resp: HttpServletResponse, actionResult: Any) {
    @tailrec def loop(ar: Any): Any = ar match {
      case _: Unit | Unit => runRenderCallbacks(req, Success(actionResult))
      case a =>
        val result = try renderPipeline(req, resp, ar) catch { case NonFatal(_) => null }
        if (result != null) loop(result)
    }
    try {
      runCallbacks(req, Success(actionResult))
      loop(actionResult)
    } catch {
      case e: Throwable =>
        runCallbacks(req, Failure(e))
        try {
          renderUncaughtException(req, resp, e)
        } finally {
          runRenderCallbacks(req, Failure(e))
        }
    }
  }

  /**
   * The render pipeline is a function of (HttpServletRequest, HttpServletResponse, Any) => Any.  It is
   * called recursively until it returns ().  () indicates that the
   * response has been rendered.
   */
  protected def renderPipeline(req: HttpServletRequest, resp: HttpServletResponse, result: Any): Any = result match {
    case 404 =>
      doNotFound(req, resp)
    case ActionResult(status, x: Int, resultHeaders) =>
      resp.status = status
      resultHeaders foreach {
        case (name, value) => resp.addHeader(name, value)
      }
      resp.writer.print(x.toString)
    case status: Int =>
      resp.status = ResponseStatus(status)
    case bytes: Array[Byte] =>
      if (contentType(resp) != null && contentType(resp).startsWith("text")) resp.setCharacterEncoding(FileCharset(bytes).name)
      resp.outputStream.write(bytes)
    case is: java.io.InputStream =>
      using(is) {
        util.io.copy(_, resp.outputStream)
      }
    case file: File =>
      if (contentType(resp) startsWith "text") resp.setCharacterEncoding(FileCharset(file).name)
      using(new FileInputStream(file)) {
        in => zeroCopy(in, resp.outputStream)
      }
    // If an action returns Unit, it assumes responsibility for the response
    case _: Unit | Unit | null =>
    // If an action returns Unit, it assumes responsibility for the response
    case ActionResult(ResponseStatus(404, _), _: Unit | Unit, _) => doNotFound(req, resp)
    case actionResult: ActionResult =>
      resp.status = actionResult.status
      actionResult.headers.foreach {
        case (name, value) => resp.addHeader(name, value)
      }
      actionResult.body
    case x =>
      resp.writer.print(x.toString)
  }

  /**
   * Pluggable way to convert a path expression to a route matcher.
   * The default implementation is compatible with Sinatra's route syntax.
   *
   * @param path a path expression
   * @return a route matcher based on `path`
   */
  protected implicit def string2RouteMatcher(path: String): RouteMatcher =
    new SinatraRouteMatcher(path)

  /**
   * Path pattern is decoupled from requests.  This adapts the PathPattern to
   * a RouteMatcher by supplying the request path.
   */
  protected implicit def pathPatternParser2RouteMatcher(pattern: PathPattern): RouteMatcher =
    new PathPatternRouteMatcher(pattern)

  /**
   * Converts a regular expression to a route matcher.
   *
   * @param regex the regular expression
   * @return a route matcher based on `regex`
   * @see [[org.scalatra.RegexRouteMatcher]]
   */
  protected implicit def regex2RouteMatcher(regex: Regex): RouteMatcher =
    new RegexRouteMatcher(regex)

//  /**
//   * Converts a boolean expression to a route matcher.
//   *
//   * @param block a block that evaluates to a boolean
//   *
//   * @return a route matcher based on `block`.  The route matcher should
//   *         return `Some` if the block is true and `None` if the block is false.
//   *
//   * @see [[org.scalatra.BooleanBlockRouteMatcher]]
//   */
//  protected implicit def booleanBlock2RouteMatcher(block: => Boolean): RouteMatcher =
//    new BooleanBlockRouteMatcher(block)

  protected def renderHaltException(req: HttpServletRequest, resp: HttpServletResponse, e: HaltException) {
    try {
      var rendered = false
      e match {
        case HaltException(Some(404), _, _, _: Unit | Unit) | HaltException(_, _, _, ActionResult(ResponseStatus(404, _), _: Unit | Unit, _)) =>
          renderResponse(req, resp, doNotFound(req, resp))
          rendered = true
        case HaltException(Some(status), Some(reason), _, _) =>
          resp.status = ResponseStatus(status, reason)
        case HaltException(Some(status), None, _, _) =>
          resp.status = ResponseStatus(status)
        case HaltException(None, _, _, _) => // leave status line alone
      }
      e.headers foreach {
        case (name, value) => resp.addHeader(name, value)
      }
      if (!rendered) renderResponse(req, resp, e.body)
    } catch {
      case e: Throwable =>
        runCallbacks(req, Failure(e))
        renderUncaughtException(req, resp, e)
        runCallbacks(req, Failure(e))
    }
  }

  protected def extractStatusCode(resp: HttpServletResponse, e: HaltException) = e match {
    case HaltException(Some(status), _, _, _) => status
    case _                                    => resp.status.code
  }

//  def get(transformers: RouteTransformer*)(action: => Any) = addRoute(Get, transformers, action)
//
//  def post(transformers: RouteTransformer*)(action: => Any) = addRoute(Post, transformers, action)
//
//  def put(transformers: RouteTransformer*)(action: => Any) = addRoute(Put, transformers, action)
//
//  def delete(transformers: RouteTransformer*)(action: => Any) = addRoute(Delete, transformers, action)
//
  def trapAction(codes: Range)(block: Action) {
    addStatusRoute(codes, block)
  }
//
//  def options(transformers: RouteTransformer*)(action: => Any) = addRoute(Options, transformers, action)
//
//  def head(transformers: RouteTransformer*)(action: => Any) = addRoute(Head, transformers, action)
//
//  def patch(transformers: RouteTransformer*)(action: => Any) = addRoute(Patch, transformers, action)

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
  def addRoute(method: HttpMethod, transformers: Seq[RouteTransformer], action: Action): Route = {
    val route = Route(transformers, action, (req: HttpServletRequest) => routeBasePath(req))
    routes.prependRoute(method, route)
    route
  }

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

  protected[scalatra] def addStatusRoute(codes: Range, action: Action) {
    val route = Route(Seq.empty, action, (req: HttpServletRequest) => routeBasePath(req))
    routes.addStatusRoute(codes, route)
  }

  /**
   * The configuration, typically a ServletConfig or FilterConfig.
   */
  var config: ConfigT = _


  /**
   * Initializes the kernel.  Used to provide context that is unavailable
   * when the instance is constructed, for example the servlet lifecycle.
   * Should set the `config` variable to the parameter.
   *
   * @param config the configuration.
   */
  def initialize(config: ConfigT) {
    this.config = config
    val path = contextPath match {
      case "" => "/" // The root servlet is "", but the root cookie path is "/"
      case p => p
    }
    servletContext(CookieSupport.CookieOptionsKey) = CookieOptions(path = path)
  }

  def relativeUrl(path: String, params: Iterable[(String, Any)] = Iterable.empty, includeContextPath: Boolean = true, includeServletPath: Boolean = true)(implicit request: HttpServletRequest, response: HttpServletResponse): String = {
    url(path, params, includeContextPath, includeServletPath, absolutize = false)
  }

  /**
   * Returns a context-relative, session-aware URL for a path and specified
   * parameters.
   * Finally, the result is run through `response.encodeURL` for a session
   * ID, if necessary.
   *
   * @param path the base path.  If a path begins with '/', then the context
   *             path will be prepended to the result
   *
   * @param params params, to be appended in the form of a query string
   *
   * @return the path plus the query string, if any.  The path is run through
   *         `response.encodeURL` to add any necessary session tracking parameters.
   */
  def url(path: String, params: Iterable[(String, Any)] = Iterable.empty, includeContextPath: Boolean = true, includeServletPath: Boolean = true, absolutize: Boolean = true, withSessionId: Boolean = true)(implicit request: HttpServletRequest, response: HttpServletResponse): String = {

    val newPath = path match {
      case x if x.startsWith("/") && includeContextPath && includeServletPath =>
        ensureSlash(routeBasePath) + ensureContextPathsStripped(ensureSlash(path))
      case x if x.startsWith("/") && includeContextPath =>
        ensureSlash(contextPath) + ensureContextPathStripped(ensureSlash(path))
      case x if x.startsWith("/") && includeServletPath => request.getServletPath.blankOption map {
        ensureSlash(_) + ensureServletPathStripped(ensureSlash(path))
      } getOrElse "/"
      case _ if absolutize => ensureContextPathsStripped(ensureSlash(path))
      case _ => path
    }

    val pairs = params map {
      case (key, None) => key.urlEncode + "="
      case (key, Some(value)) => key.urlEncode + "=" + value.toString.urlEncode
      case (key, value) => key.urlEncode + "=" + value.toString.urlEncode
    }
    val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
    if (withSessionId) addSessionId(newPath + queryString) else newPath + queryString
  }

  private[this] def ensureContextPathsStripped(path: String)(implicit request: HttpServletRequest) =
    ((ensureContextPathStripped _) andThen (ensureServletPathStripped _))(path)

  private[this] def ensureServletPathStripped(path: String)(implicit request: HttpServletRequest) = {
    val sp = ensureSlash(request.getServletPath.blankOption getOrElse "")
    val np = if (path.startsWith(sp + "/")) path.substring(sp.length) else path
    ensureSlash(np)
  }

  private[this] def ensureContextPathStripped(path: String) = {
    val cp = ensureSlash(contextPath)
    val np = if (path.startsWith(cp + "/")) path.substring(cp.length) else path
    ensureSlash(np)
  }

  private[this] def ensureSlash(candidate: String) = {
    val p = if (candidate.startsWith("/")) candidate else "/" + candidate
    if (p.endsWith("/")) p.dropRight(1) else p
  }


  protected def isHttps(implicit request: HttpServletRequest) = {
    // also respect load balancer version of the protocol
    val h = request.getHeader("X-Forwarded-Proto").blankOption
    request.isSecure || (h.isDefined && h.forall(_ equalsIgnoreCase "HTTPS"))
  }

  protected def needsHttps =
    allCatch.withApply(_ => false) {
      servletContext.getInitParameter(ForceHttpsKey).blankOption.map(_.toBoolean) getOrElse false
    }

  /**
   * Sends a redirect response and immediately halts the current action.
   */
  def redirect(uri: String)(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    halt(Found(fullUrl(uri, includeServletPath = false)))
  }

  /**
   * The base path for URL generation
   */
  protected def routeBasePath(implicit request: HttpServletRequest): String

  /**
   * Builds a full URL from the given relative path. Takes into account the port configuration, https, ...
   *
   * @param path a relative path
   *
   * @return the full URL
   */
  def fullUrl(path: String, params: Iterable[(String, Any)] = Iterable.empty, includeContextPath: Boolean = true, includeServletPath: Boolean = true, withSessionId: Boolean = true)(implicit request: HttpServletRequest, response: HttpServletResponse) = {
    if (path.startsWith("http")) path
    else {
      val p = url(path, params, includeContextPath, includeServletPath, withSessionId)
      if (p.startsWith("http")) p else buildBaseUrl + ensureSlash(p)
    }
  }

  private[this] def buildBaseUrl(implicit request: HttpServletRequest) = {
    "%s://%s".format(
      if (needsHttps || isHttps) "https" else "http",
      serverAuthority
    )
  }

  private[this] def serverAuthority(implicit request: HttpServletRequest) = {
    val p = serverPort
    val h = serverHost
    if (p == 80 || p == 443) h else h + ":" + p.toString
  }

  def serverHost(implicit request: HttpServletRequest) = {
    initParameter(HostNameKey).flatMap(_.blankOption) getOrElse request.getServerName
  }

  def serverPort(implicit request: HttpServletRequest) = {
    initParameter(PortKey).flatMap(_.blankOption).map(_.toInt) getOrElse request.getServerPort
  }

  protected def contextPath: String = servletContext.contextPath

  /**
   * Gets an init paramter from the config.
   *
   * @param name the name of the key
   *
   * @return an option containing the value of the parameter if defined, or
   *         `None` if the parameter is not set.
   */
  def initParameter(name: String): Option[String] =
    config.initParameters.get(name) orElse {
      servletContext.initParameters.get(name)
    }

  def environment: String = sys.props.get(EnvironmentKey) orElse initParameter(EnvironmentKey) getOrElse "DEVELOPMENT"

  /**
   * A boolean flag representing whether the kernel is in development mode.
   * The default is true if the `environment` begins with "dev", case-insensitive.
   */
  def isDevelopmentMode = environment.toUpperCase.startsWith("DEV")


  /**
   * The effective path against which routes are matched.  The definition
   * varies between servlets and filters.
   */
  def requestPath(implicit request: HttpServletRequest): String

  protected def addSessionId(uri: String)(implicit response: HttpServletResponse): String = response.encodeURL(uri)

  def multiParams(key: String)(implicit request: HttpServletRequest): Seq[String] = multiParams(request)(key)
  /**
   * The current multiparams.  Multiparams are a result of merging the
   * standard request params (query string or post params) with the route
   * parameters extracted from the route matchers of the current route.
   * The default value for an unknown param is the empty sequence.  Invalid
   * outside `handle`.
   */
  def multiParams(implicit request: HttpServletRequest): MultiParams = {
    val read = request.contains("MultiParamsRead")
    val found = request.get(MultiParamsKey) map (
     _.asInstanceOf[MultiParams] ++ (if (read) Map.empty else request.multiParameters)
    )
    val multi = found getOrElse request.multiParameters
    request("MultiParamsRead") = new {}
    request(MultiParamsKey) = multi
    multi.withDefaultValue(Seq.empty)
  }

  def params(key: String)(implicit request: HttpServletRequest): String = params(request)(key)
  def params(key: Symbol)(implicit request: HttpServletRequest): String = params(request)(key)
  def params(implicit request: HttpServletRequest): Params = new ScalatraParams(multiParams)
}
