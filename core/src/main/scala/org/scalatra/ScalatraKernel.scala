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

  @deprecated("Use HttpMethods.methods")
  val httpMethods = HttpMethod.methods map { _.toString }

  @deprecated("Use HttpMethods.methods filter { !_.isSafe }")
  val writeMethods = HttpMethod.methods filter { !_.isSafe } map { _.toString }

  @deprecated("Use CsrfTokenSupport.DefaultKey")
  val csrfKey = CsrfTokenSupport.DefaultKey

  val EnvironmentKey = "org.scalatra.environment"
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
trait ScalatraKernel extends Handler with Initializable
{
  protected val routes: ConcurrentMap[HttpMethod, List[Route]] = {
    val map = new ConcurrentHashMap[HttpMethod, List[Route]]
    HttpMethod.methods foreach { x: HttpMethod => map += ((x, List[Route]())) }
    map
  }
  protected val beforeFilters = ListBuffer[Route]()
  protected val afterFilters = ListBuffer[Route]()

  def contentType = response.getContentType
  def contentType_=(value: String) {
    response.setContentType(value)
  }

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
    SinatraPathPatternParser(path)

  /**
   * Path pattern is decoupled from requests.  This adapts the PathPattern to
   * a RouteMatcher by supplying the request path.
   */
  protected implicit def pathPatternParser2RouteMatcher(pattern: PathPattern): RouteMatcher =
    new RouteMatcher {
      def apply() = pattern(requestPath)

      // By overriding toString, we can list the available routes in the
      // default notFound handler.
      override def toString = pattern.regex.toString
    }

  protected implicit def regex2RouteMatcher(regex: Regex): RouteMatcher = new RouteMatcher {
    def apply() = regex.findFirstMatchIn(requestPath) map { _.subgroups match {
      case Nil => map2multiMap(Map.empty)
      case xs => Map("captures" -> xs)
    }}

    override def toString = regex.toString
  }

  protected implicit def booleanBlock2RouteMatcher(matcher: => Boolean): RouteMatcher =
    () => { if (matcher) Some(MultiMap()) else None }

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
            runFilters(beforeFilters)
            val actionResult = runRoutes(routes(effectiveMethod)).headOption
            actionResult getOrElse doNotFound()
          }
          catch {
            case e => handleError(e)
          }
          finally {
            runFilters(afterFilters)
          }
          renderResponse(result)
        }
      }
    }
  }

  private def runFilters(filters: Traversable[Route]) =
    for { 
      route <- filters
      matchedRoute <- route() 
    } invoke(matchedRoute)

  private def runRoutes(routes: Traversable[Route]) = 
    for { 
      route <- routes.toStream // toStream makes it lazy so we stop after match
      matchedRoute <- route()
      actionResult <- invoke(matchedRoute)
    } yield actionResult

  private def invoke(matchedRoute: MatchedRoute) = 
    _multiParams.withValue(multiParams ++ matchedRoute.multiParams) {
      try {
        Some(matchedRoute.action())
      }
      catch {
        case e: PassException => None
      }
    }

  protected def effectiveMethod: HttpMethod =
    HttpMethod(request.getMethod) match {
      case Head => Get
      case x => x
    }

  def requestPath: String
  
  def beforeAll(fun: => Any) = addBefore(Iterable.empty, fun)
  @deprecated("Use beforeAll")
  def before(fun: => Any) = beforeAll(fun)
  
  def beforeSome(routeMatchers: RouteMatcher*)(fun: => Any) = addBefore(routeMatchers, fun)
  
  protected def addBefore(routeMatchers: Iterable[RouteMatcher], fun: => Any): Unit = {
    val route = new Route(routeMatchers, () => fun)
    beforeFilters += route
  }

  def afterAll(fun: => Any) = addAfter(Iterable.empty, fun)
  @deprecated("Use afterAll")
  def after(fun: => Any) = afterAll(fun)
  
  def afterSome(routeMatchers: RouteMatcher*)(fun: => Any) = addAfter(routeMatchers, fun)
  
  protected def addAfter(routeMatchers: Iterable[RouteMatcher], fun: => Any): Unit = {
    val route = new Route(routeMatchers, () => fun)
    afterFilters += route
  }
  
  protected var doNotFound: Action
  def notFound(fun: => Any) = doNotFound = { () => fun }

  protected def handleError(e: Throwable): Any = {
    (renderError orElse defaultRenderError).apply(e)
  }

  protected def renderError : PartialFunction[Throwable, Any] = defaultRenderError

  protected final def defaultRenderError : PartialFunction[Throwable, Any] = {
    case e: HaltException => renderHaltException(e)
    case e => {
      status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
      _caughtThrowable.withValue(e) { errorHandler() }
    }
  }

  protected var errorHandler: Action = { () => throw caughtThrowable }
  def error(fun: => Any) = errorHandler = { () => fun }

  private val _caughtThrowable = new DynamicVariable[Throwable](null)
  protected def caughtThrowable = _caughtThrowable.value

  protected def renderResponse(actionResult: Any) {
    if (contentType == null)
      contentType = inferContentType(actionResult)
    renderResponseBody(actionResult)
  }

  type ContentTypeInferrer = PartialFunction[Any, String]
  
  protected def defaultContentTypeInfer: ContentTypeInferrer = {
    case _: String => "text/plain"
    case _: Array[Byte] => "application/octet-stream"
    case _ => "text/html"
  }
  protected def contentTypeInfer: ContentTypeInferrer = defaultContentTypeInfer

  protected def inferContentType(actionResult: Any): String =
    (contentTypeInfer orElse defaultContentTypeInfer).apply(actionResult)

  protected def renderResponseBody(actionResult: Any) {
    @tailrec def loop(ar: Any): Any = ar match {
      case r: Unit => r
      case a => loop((renderPipeline orElse defaultRenderResponse) apply a)
    }
    loop(actionResult)
  }

  protected def renderPipeline: PartialFunction[Any, Any] = defaultRenderResponse

  protected final def defaultRenderResponse: PartialFunction[Any, Any] = {
    case bytes: Array[Byte] =>
      response.getOutputStream.write(bytes)
    case file: File =>
      using(new FileInputStream(file)) { in => zeroCopy(in, response.getOutputStream) }
    case _: Unit =>
    // If an action returns Unit, it assumes responsibility for the response
    case x: Any  =>
      response.getWriter.print(x.toString)
  }

  protected[scalatra] val _multiParams = new DynamicVariable[MultiMap](null)
  protected def multiParams: MultiParams = (_multiParams.value).withDefaultValue(Seq.empty)
  /*
   * Assumes that there is never a null or empty value in multiParams.  The servlet container won't put them
   * in request.getParameters, and we shouldn't either.
   */
  protected val _params = new MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] {
    protected def multiMap = multiParams
  }
  def params = _params

  def redirect(uri: String) = (_response value) sendRedirect uri
  implicit def request = _request value
  implicit def response = _response value
  def session = request.getSession
  def sessionOption = request.getSession(false) match {
    case s: HttpSession => Some(s)
    case null => None
  }
  def status(code: Int) = (_response value) setStatus code

  /**
   * Immediately halts the current action.  If called within a before filter,
   * prevents the action from executing.  Any matching after filters will still
   * execute.
   *
   * @param status set as the response's HTTP status code.  Ignored if null.
   *
   * @param body rendered to the response body through the response pipeline.
   *
   * @param reason set as the HTTP status reason.  Ignored if null or if status
   * is null.
   *
   * @param headers added as headers to the response.  Previously set headers 
   * are retained
   */
  def halt(status: JInteger = null, 
           body: Any = (), 
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

  /**
   * The Scalatra DSL core methods take a list of [[org.scalatra.RouteMatcher]] and a block as
   * the action body.
   * The return value of the block is converted to a string and sent to the client as the response body.
   *
   * See [[org.scalatra.ScalatraKernel.renderResponseBody]] for the detailed behaviour and how to handle your
   * response body more explicitly, and see how different return types are handled.
   *
   * The block is executed in the context of the ScalatraKernel instance, so all the methods defined in
   * this trait are also available inside the block.
   *
   * {{{
   *   get("/") {
   *     <form action="/echo">
   *       <label>Enter your name</label>
   *       <input type="text" name="name"/>
   *     </form>
   *   }
   *
   *   post("/echo") {
   *     "hello {params('name)}!"
   *   }
   * }}}
   *
   * ScalatraKernel provides implicit transformation from boolean blocks, strings and regular expressions
   * to [[org.scalatra.RouteMatcher]], so you can write code naturally
   * {{{
   *   get("/", request.getRemoteHost == "127.0.0.1") { "Hello localhost!" }
   * }}}
   *
   */
  def get(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Get, routeMatchers, action)

  /**
   * @see [[org.scalatra.ScalatraKernel.get]]
   */
  def post(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Post, routeMatchers, action)

  /**
   * @see [[org.scalatra.ScalatraKernel.get]]
   */
  def put(routeMatchers: RouteMatcher*)(action: => Any) = addRoute(Put, routeMatchers, action)

  /**
   * @see [[org.scalatra.ScalatraKernel.get]]
   */
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
   * registers a new route for the given HTTP method, can be overriden so that subtraits can use their own logic
   * for example, restricting protocol usage, namespace routes based on class name, raise errors on overlapping entries
   * etc.
   *
   * This is the method invoked by get(), post() etc.
   *
   * @see removeRoute
   */
  protected def addRoute(method: HttpMethod, routeMatchers: Iterable[RouteMatcher], action: => Any): Route = {
    val route = new Route(routeMatchers, () => action)
    modifyRoutes(method, route :: _ )
    route
  }

  @deprecated("Use addRoute(HttpMethod, Iterable[RouteMatcher], =>Any)")
  protected[scalatra] def addRoute(verb: String, routeMatchers: Iterable[RouteMatcher], action: => Any): Route =
    addRoute(HttpMethod(verb), routeMatchers, action)

  /**
   * removes _all_ the actions of a given route for a given HTTP method.
   * If [[addRoute]] is overriden this should probably be overriden too.
   *
   * @see addRoute
   */
  protected def removeRoute(method: HttpMethod, route: Route): Unit = {
    modifyRoutes(method, _ filterNot (_ == route) )
    route
  }

  protected def removeRoute(verb: String, route: Route): Unit =
    removeRoute(HttpMethod(verb), route)

  /**
   * since routes is a ConcurrentMap and we avoid locking, we need to retry if there are
   * concurrent modifications, this is abstracted here for removeRoute and addRoute
   */
  @tailrec private def modifyRoutes(method: HttpMethod, f: (List[Route] => List[Route])): Unit = {
    val oldRoutes = routes(method)
    if (!routes.replace(method, oldRoutes, f(oldRoutes))) {
      modifyRoutes(method,f)
    }
  }

  private var config: Config = _
  def initialize(config: Config) = this.config = config

  def initParameter(name: String): Option[String] = config match {
    case config: ServletConfig => Option(config.getInitParameter(name))
    case config: FilterConfig => Option(config.getInitParameter(name))
    case _ => None
  }

  protected def servletContext: ServletContext

  def environment: String = System.getProperty(EnvironmentKey, initParameter(EnvironmentKey).getOrElse("development"))
  def isDevelopmentMode = environment.toLowerCase.startsWith("dev")

  /**
   * Uniquely identifies this ScalatraKernel inside the webapp.
   */
  def kernelName: String
}
