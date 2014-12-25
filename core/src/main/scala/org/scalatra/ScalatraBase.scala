package org.scalatra

import java.io.{ File, FileInputStream }
import javax.servlet.http.{ HttpServlet, HttpServletRequest, HttpServletResponse }
import javax.servlet.{ ServletRegistration, Filter, ServletContext }

import org.scalatra.ScalatraBase._
import org.scalatra.servlet.ServletApiImplicits
import org.scalatra.util.RicherString._
import org.scalatra.util._
import org.scalatra.util.conversion.DefaultImplicitConversions
import org.scalatra.util.io.zeroCopy

import scala.annotation.tailrec
import scala.util.control.Exception._
import scala.util.matching.Regex
import scala.util.{ Failure, Success, Try }

object ScalatraBase {

  import org.scalatra.servlet.ServletApiImplicits._
  import scala.collection.JavaConverters._

  /**
   * A key for request attribute that contains any exception
   * that might have occured before the handling has been
   * propagated to ScalatraBase#handle (such as in
   * FileUploadSupport)
   */
  val PrehandleExceptionKey: String = "org.scalatra.PrehandleException"
  val HostNameKey: String = "org.scalatra.HostName"
  val PortKey: String = "org.scalatra.Port"
  val ForceHttpsKey: String = "org.scalatra.ForceHttps"

  private[this] val KeyPrefix: String = classOf[FutureSupport].getName
  val Callbacks: String = s"$KeyPrefix.callbacks"
  val RenderCallbacks: String = s"$KeyPrefix.renderCallbacks"
  val IsAsyncKey: String = s"$KeyPrefix.isAsync"

  def isAsyncResponse(implicit request: HttpServletRequest): Boolean = request.get(IsAsyncKey).exists(_ => true)

  def onSuccess(fn: Any => Unit)(implicit request: HttpServletRequest): Unit = addCallback(_.foreach(fn))

  def onFailure(fn: Throwable => Unit)(implicit request: HttpServletRequest): Unit = addCallback(_.failed.foreach(fn))

  def onCompleted(fn: Try[Any] => Unit)(implicit request: HttpServletRequest): Unit = addCallback(fn)

  def onRenderedSuccess(fn: Any => Unit)(implicit request: HttpServletRequest): Unit = addRenderCallback(_.foreach(fn))

  def onRenderedFailure(fn: Throwable => Unit)(implicit request: HttpServletRequest): Unit = addRenderCallback(_.failed.foreach(fn))

  def onRenderedCompleted(fn: Try[Any] => Unit)(implicit request: HttpServletRequest): Unit = addRenderCallback(fn)

  def callbacks(implicit request: HttpServletRequest): List[(Try[Any]) => Unit] =
    request.getOrElse(Callbacks, List.empty[Try[Any] => Unit]).asInstanceOf[List[Try[Any] => Unit]]

  def addCallback(callback: Try[Any] => Unit)(implicit request: HttpServletRequest): Unit = {
    request(Callbacks) = callback :: callbacks
  }

  def runCallbacks(data: Try[Any])(implicit request: HttpServletRequest): Unit = {
    callbacks.reverse foreach (_(data))
  }

  def renderCallbacks(implicit request: HttpServletRequest): List[(Try[Any]) => Unit] = {
    request.getOrElse(RenderCallbacks, List.empty[Try[Any] => Unit]).asInstanceOf[List[Try[Any] => Unit]]
  }

  def addRenderCallback(callback: Try[Any] => Unit)(implicit request: HttpServletRequest): Unit = {
    request(RenderCallbacks) = callback :: renderCallbacks
  }

  def runRenderCallbacks(data: Try[Any])(implicit request: HttpServletRequest): Unit = {
    renderCallbacks.reverse foreach (_(data))
  }

  def getServletRegistration(app: ScalatraBase): Option[ServletRegistration] = {
    val registrations = app.servletContext.getServletRegistrations.values().asScala.toList
    registrations.find(_.getClassName == app.getClass.getName)
  }

}

/**
 * The base implementation of the Scalatra DSL.  Intended to be portable
 * to all supported backends.
 */
trait ScalatraBase
    extends ScalatraContext
    with CoreDsl
    with DynamicScope
    with Initializable
    with ServletApiImplicits
    with ScalatraParamsImplicits
    with DefaultImplicitConversions
    with SessionSupport {

  @deprecated("Use servletContext instead", "2.1.0")
  def applicationContext: ServletContext = servletContext

  /**
   * The routes registered in this kernel.
   */
  lazy val routes: RouteRegistry = new RouteRegistry

  /**
   * The default character encoding for requests and responses.
   */
  protected val defaultCharacterEncoding: String = "UTF-8"

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
  override def handle(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    request(CookieSupport.SweetCookiesKey) = new SweetCookies(request.cookies, response)
    response.characterEncoding = Some(defaultCharacterEncoding)
    withRequestResponse(request, response) {
      executeRoutes()
    }
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
  protected def executeRoutes(): Unit = {
    var result: Any = null
    var rendered = true

    def runActions = {
      val prehandleException = request.get(PrehandleExceptionKey)
      if (prehandleException.isEmpty) {
        val (rq, rs) = (request, response)
        onCompleted { _ =>
          withRequestResponse(rq, rs) {
            this match {
              case f: Filter if !rq.contains("org.scalatra.ScalatraFilter.afterFilters.Run") =>
                rq("org.scalatra.ScalatraFilter.afterFilters.Run") = new {}
                runFilters(routes.afterFilters)
              case f: HttpServlet if !rq.contains("org.scalatra.ScalatraServlet.afterFilters.Run") =>
                rq("org.scalatra.ScalatraServlet.afterFilters.Run") = new {}
                runFilters(routes.afterFilters)
              case _ =>
            }
          }
        }
        runFilters(routes.beforeFilters)
        val actionResult = runRoutes(routes(request.requestMethod)).headOption
        // Give the status code handler a chance to override the actionResult
        val r = handleStatusCode(status) getOrElse {
          actionResult orElse matchOtherMethods() getOrElse doNotFound()
        }
        rendered = false
        r
      } else {
        throw prehandleException.get.asInstanceOf[Exception]
      }
    }

    cradleHalt(result = runActions, e => {
      cradleHalt({
        result = errorHandler(e)
        rendered = false
      }, e => {
        runCallbacks(Failure(e))
        try {
          renderUncaughtException(e)
        } finally {
          runRenderCallbacks(Failure(e))
        }
      })
    })

    if (!rendered) renderResponse(result)
  }

  private[this] def cradleHalt(body: => Any, error: Throwable => Any): Any = {
    try body
    catch {
      case e: HaltException => {
        try {
          handleStatusCode(extractStatusCode(e)) match {
            case Some(result) => renderResponse(result)
            case _ => renderHaltException(e)
          }
        } catch {
          case e: HaltException => renderHaltException(e)
          case e: Throwable => error(e)
        }
      }
      case e: Throwable => error(e)
    }
  }

  protected def renderUncaughtException(e: Throwable)(
    implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
    status = 500
    if (isDevelopmentMode) {
      contentType = "text/plain"
      e.printStackTrace(response.getWriter)
    }
  }

  protected def isAsyncExecutable(result: Any): Boolean = false

  /**
   * Invokes each filters with `invoke`.  The results of the filters
   * are discarded.
   */
  protected def runFilters(filters: Traversable[Route]): Unit = {
    for {
      route <- filters
      matchedRoute <- route(requestPath)
    } invoke(matchedRoute)
  }

  /**
   * Lazily invokes routes with `invoke`.  The results of the routes
   * are returned as a stream.
   */
  protected def runRoutes(routes: Traversable[Route]): Stream[Any] = {
    for {
      route <- routes.toStream // toStream makes it lazy so we stop after match
      matchedRoute <- route.apply(requestPath)
      saved = saveMatchedRoute(matchedRoute)
      actionResult <- invoke(saved)
    } yield actionResult
  }

  private[scalatra] def saveMatchedRoute(matchedRoute: MatchedRoute): MatchedRoute = {
    request("org.scalatra.MatchedRoute") = matchedRoute
    setMultiparams(Some(matchedRoute), multiParams)
    matchedRoute
  }

  private[scalatra] def matchedRoute(implicit request: HttpServletRequest): Option[MatchedRoute] = {
    request.get("org.scalatra.MatchedRoute").map(_.asInstanceOf[MatchedRoute])
  }

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
  protected def invoke(matchedRoute: MatchedRoute): Option[Any] = {
    withRouteMultiParams(Some(matchedRoute)) {
      liftAction(matchedRoute.action)
    }
  }

  private def liftAction(action: Action): Option[Any] = {
    try {
      Some(action())
    } catch {
      case e: PassException => None
    }
  }

  def before(transformers: RouteTransformer*)(fun: => Any): Unit = {
    routes.appendBeforeFilter(Route(transformers, () => fun))
  }

  def after(transformers: RouteTransformer*)(fun: => Any): Unit = {
    routes.appendAfterFilter(Route(transformers, () => fun))
  }

  /**
   * Called if no route matches the current request for any method.  The
   * default implementation varies between servlet and filter.
   */
  protected var doNotFound: Action

  def notFound(fun: => Any): Unit = {
    doNotFound = {
      () => fun
    }
  }

  /**
   * Called if no route matches the current request method, but routes
   * match for other methods.  By default, sends an HTTP status of 405
   * and an `Allow` header containing a comma-delimited list of the allowed
   * methods.
   */
  protected var doMethodNotAllowed: (Set[HttpMethod] => Any) = {
    allow =>
      status = 405
      response.headers("Allow") = allow.mkString(", ")
  }

  def methodNotAllowed(f: Set[HttpMethod] => Any): Unit = {
    doMethodNotAllowed = f
  }

  private[this] def matchOtherMethods(): Option[Any] = {
    val allow = routes.matchingMethodsExcept(request.requestMethod, requestPath)
    if (allow.isEmpty) None else liftAction(() => doMethodNotAllowed(allow))
  }

  private[this] def handleStatusCode(status: Int): Option[Any] = {
    for {
      handler <- routes(status)
      matchedHandler <- handler(requestPath)
      handlerResult <- invoke(matchedHandler)
    } yield handlerResult
  }

  /**
   * The error handler function, called if an exception is thrown during
   * before filters or the routes.
   */
  protected var errorHandler: ErrorHandler = {
    case t => throw t
  }

  def error(handler: ErrorHandler): Unit = {
    errorHandler = handler orElse errorHandler
  }

  protected def withRouteMultiParams[S](matchedRoute: Option[MatchedRoute])(thunk: => S): S = {
    val originalParams = multiParams
    setMultiparams(matchedRoute, originalParams)
    try {
      thunk
    } finally {
      request(MultiParamsKey) = originalParams
    }
  }

  protected def setMultiparams[S](matchedRoute: Option[MatchedRoute], originalParams: MultiParams)(
    implicit request: HttpServletRequest): Unit = {
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
  protected def renderResponse(actionResult: Any): Unit = {
    if (contentType == null)
      contentTypeInferrer.lift(actionResult) foreach {
        contentType = _
      }

    renderResponseBody(actionResult)
  }

  /**
   * A partial function to infer the content type from the action result.
   *
   * @return
   * $ - "text/plain" for String
   * $ - "application/octet-stream" for a byte array
   * $ - "text/html" for any other result
   */
  protected def contentTypeInferrer: ContentTypeInferrer = {
    case s: String => "text/plain"
    case bytes: Array[Byte] => MimeTypes(bytes)
    case is: java.io.InputStream => MimeTypes(is)
    case file: File => MimeTypes(file)
    case actionResult: ActionResult =>
      actionResult.headers.find {
        case (name, value) => name equalsIgnoreCase "CONTENT-TYPE"
      }.getOrElse(("Content-Type", contentTypeInferrer(actionResult.body)))._2
    //    case Unit | _: Unit => null
    case _ => "text/html"
  }

  /**
   * Renders the action result to the response body via the render pipeline.
   *
   * @see #renderPipeline
   */
  protected def renderResponseBody(actionResult: Any): Unit = {
    @tailrec def loop(ar: Any): Any = ar match {
      case _: Unit | Unit => runRenderCallbacks(Success(actionResult))
      case a => loop(renderPipeline.lift(a).getOrElse(()))
    }
    try {
      runCallbacks(Success(actionResult))
      loop(actionResult)
    } catch {
      case e: Throwable =>
        runCallbacks(Failure(e))
        try {
          renderUncaughtException(e)
        } finally {
          runRenderCallbacks(Failure(e))
        }
    }
  }

  /**
   * The render pipeline is a partial function of Any => Any.  It is
   * called recursively until it returns ().  () indicates that the
   * response has been rendered.
   */
  protected def renderPipeline: RenderPipeline = {
    case 404 =>
      doNotFound()
    case ActionResult(status, x: Int, resultHeaders) =>
      response.status = status
      resultHeaders foreach {
        case (name, value) => response.addHeader(name, value)
      }
      response.writer.print(x.toString)
    case status: Int =>
      response.status = ResponseStatus(status)
    case bytes: Array[Byte] =>
      if (contentType != null && contentType.startsWith("text")) response.setCharacterEncoding(FileCharset(bytes).name)
      response.outputStream.write(bytes)
    case is: java.io.InputStream =>
      using(is) {
        util.io.copy(_, response.outputStream)
      }
    case file: File =>
      if (contentType startsWith "text") response.setCharacterEncoding(FileCharset(file).name)
      using(new FileInputStream(file)) {
        in => zeroCopy(in, response.outputStream)
      }
    // If an action returns Unit, it assumes responsibility for the response
    case _: Unit | Unit | null =>
    // If an action returns Unit, it assumes responsibility for the response
    case ActionResult(ResponseStatus(404, _), _: Unit | Unit, _) => doNotFound()
    case actionResult: ActionResult =>
      response.status = actionResult.status
      actionResult.headers.foreach {
        case (name, value) => response.addHeader(name, value)
      }
      actionResult.body
    case x =>
      response.writer.print(x.toString)
  }

  /**
   * Pluggable way to convert a path expression to a route matcher.
   * The default implementation is compatible with Sinatra's route syntax.
   *
   * @param path a path expression
   * @return a route matcher based on `path`
   */
  protected implicit def string2RouteMatcher(path: String): RouteMatcher = {
    new SinatraRouteMatcher(path)
  }

  /**
   * Path pattern is decoupled from requests.  This adapts the PathPattern to
   * a RouteMatcher by supplying the request path.
   */
  protected implicit def pathPatternParser2RouteMatcher(pattern: PathPattern): RouteMatcher = {
    new PathPatternRouteMatcher(pattern)
  }

  /**
   * Converts a regular expression to a route matcher.
   *
   * @param regex the regular expression
   * @return a route matcher based on `regex`
   * @see [[org.scalatra.RegexRouteMatcher]]
   */
  protected implicit def regex2RouteMatcher(regex: Regex): RouteMatcher = {
    new RegexRouteMatcher(regex)
  }

  /**
   * Converts a boolean expression to a route matcher.
   *
   * @param block a block that evaluates to a boolean
   *
   * @return a route matcher based on `block`.  The route matcher should
   *         return `Some` if the block is true and `None` if the block is false.
   *
   * @see [[org.scalatra.BooleanBlockRouteMatcher]]
   */
  protected implicit def booleanBlock2RouteMatcher(block: => Boolean): RouteMatcher = {
    new BooleanBlockRouteMatcher(block)
  }

  protected def renderHaltException(e: HaltException): Unit = {
    try {
      var rendered = false
      e match {
        case HaltException(Some(404), _, _, _: Unit | Unit) |
          HaltException(_, _, _, ActionResult(ResponseStatus(404, _), _: Unit | Unit, _)) =>
          renderResponse(doNotFound())
          rendered = true
        case HaltException(Some(status), Some(reason), _, _) =>
          response.status = ResponseStatus(status, reason)
        case HaltException(Some(status), None, _, _) =>
          response.status = ResponseStatus(status)
        case HaltException(None, _, _, _) => // leave status line alone
      }
      e.headers foreach {
        case (name, value) => response.addHeader(name, value)
      }
      if (!rendered) renderResponse(e.body)
    } catch {
      case e: Throwable =>
        runCallbacks(Failure(e))
        renderUncaughtException(e)
        runCallbacks(Failure(e))
    }
  }

  protected def extractStatusCode(e: HaltException): Int = e match {
    case HaltException(Some(status), _, _, _) => status
    case _ => response.status.code
  }

  def get(transformers: RouteTransformer*)(action: => Any): Route = addRoute(Get, transformers, action)

  def post(transformers: RouteTransformer*)(action: => Any): Route = addRoute(Post, transformers, action)

  def put(transformers: RouteTransformer*)(action: => Any): Route = addRoute(Put, transformers, action)

  def delete(transformers: RouteTransformer*)(action: => Any): Route = addRoute(Delete, transformers, action)

  def trap(codes: Range)(block: => Any): Unit = {
    addStatusRoute(codes, block)
  }

  def options(transformers: RouteTransformer*)(action: => Any): Route = addRoute(Options, transformers, action)

  def head(transformers: RouteTransformer*)(action: => Any): Route = addRoute(Head, transformers, action)

  def patch(transformers: RouteTransformer*)(action: => Any): Route = addRoute(Patch, transformers, action)

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
    val route = Route(transformers, () => action, (req: HttpServletRequest) => routeBasePath(req))
    routes.prependRoute(method, route)
    route
  }

  /**
   * Removes _all_ the actions of a given route for a given HTTP method.
   * If addRoute is overridden then this should probably be overriden too.
   *
   * @see org.scalatra.ScalatraKernel#addRoute
   */
  protected def removeRoute(method: HttpMethod, route: Route): Unit = {
    routes.removeRoute(method, route)
  }

  protected def removeRoute(method: String, route: Route): Unit = {
    removeRoute(HttpMethod(method), route)
  }

  protected[scalatra] def addStatusRoute(codes: Range, action: => Any): Unit = {
    val route = Route(Seq.empty, () => action, (req: HttpServletRequest) => routeBasePath(req))
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
  def initialize(config: ConfigT): Unit = {
    this.config = config
    val path = contextPath match {
      case "" => "/" // The root servlet is "", but the root cookie path is "/"
      case p => p
    }
    servletContext(CookieSupport.CookieOptionsKey) = CookieOptions(path = path)
  }

  def relativeUrl(
    path: String,
    params: Iterable[(String, Any)] = Iterable.empty,
    includeContextPath: Boolean = true,
    includeServletPath: Boolean = true)(
      implicit request: HttpServletRequest, response: HttpServletResponse): String = {
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
  def url(
    path: String,
    params: Iterable[(String, Any)] = Iterable.empty,
    includeContextPath: Boolean = true,
    includeServletPath: Boolean = true,
    absolutize: Boolean = true,
    withSessionId: Boolean = true)(
      implicit request: HttpServletRequest, response: HttpServletResponse): String = {

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

  private[this] def ensureContextPathsStripped(path: String)(implicit request: HttpServletRequest): String = {
    ((ensureContextPathStripped _) andThen (ensureServletPathStripped _))(path)
  }

  private[this] def ensureServletPathStripped(path: String)(implicit request: HttpServletRequest): String = {
    val sp = ensureSlash(request.getServletPath.blankOption getOrElse "")
    val np = if (path.startsWith(sp + "/")) path.substring(sp.length) else path
    ensureSlash(np)
  }

  private[this] def ensureContextPathStripped(path: String): String = {
    val cp = ensureSlash(contextPath)
    val np = if (path.startsWith(cp + "/")) path.substring(cp.length) else path
    ensureSlash(np)
  }

  private[this] def ensureSlash(candidate: String): String = {
    val p = if (candidate.startsWith("/")) candidate else "/" + candidate
    if (p.endsWith("/")) p.dropRight(1) else p
  }

  protected def isHttps(implicit request: HttpServletRequest): Boolean = {
    // also respect load balancer version of the protocol
    val h = request.getHeader("X-Forwarded-Proto").blankOption
    request.isSecure || (h.isDefined && h.forall(_ equalsIgnoreCase "HTTPS"))
  }

  protected def needsHttps: Boolean = {
    allCatch.withApply(_ => false) {
      servletContext.getInitParameter(ForceHttpsKey).blankOption.map(_.toBoolean) getOrElse false
    }
  }

  /**
   * Sends a redirect response and immediately halts the current action.
   */
  def redirect(uri: String)(implicit request: HttpServletRequest, response: HttpServletResponse): Nothing = {
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
  def fullUrl(
    path: String,
    params: Iterable[(String, Any)] = Iterable.empty,
    includeContextPath: Boolean = true,
    includeServletPath: Boolean = true,
    withSessionId: Boolean = true)(
      implicit request: HttpServletRequest, response: HttpServletResponse): String = {
    if (path.startsWith("http")) path
    else {
      val p = url(path, params, includeContextPath, includeServletPath, withSessionId)
      if (p.startsWith("http")) p else buildBaseUrl + ensureSlash(p)
    }
  }

  private[this] def buildBaseUrl(implicit request: HttpServletRequest): String = {
    "%s://%s".format(
      if (needsHttps || isHttps) "https" else "http",
      serverAuthority
    )
  }

  private[this] def serverAuthority(implicit request: HttpServletRequest): String = {
    val p = serverPort
    val h = serverHost
    if (p == 80 || p == 443) h else h + ":" + p.toString
  }

  def serverHost(implicit request: HttpServletRequest): String = {
    initParameter(HostNameKey).flatMap(_.blankOption) getOrElse request.getServerName
  }

  def serverPort(implicit request: HttpServletRequest): Int = {
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
  def initParameter(name: String): Option[String] = {
    config.initParameters.get(name) orElse {
      servletContext.initParameters.get(name)
    }
  }

  def environment: String = {
    sys.props.get(EnvironmentKey) orElse initParameter(EnvironmentKey) getOrElse "DEVELOPMENT"
  }

  /**
   * A boolean flag representing whether the kernel is in development mode.
   * The default is true if the `environment` begins with "dev", case-insensitive.
   */
  def isDevelopmentMode: Boolean = environment.toUpperCase.startsWith("DEV")

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
