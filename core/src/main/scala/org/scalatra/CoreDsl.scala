package org.scalatra

import java.lang.{Integer => JInteger}
import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}

import ScalatraKernel.MultiParams

/**
 * The core DSL of a Scalatra application.
 */
trait CoreDsl {
  /**
   * The current servlet context
   */
  implicit def servletContext: ServletContext

  /**
   * The current request
   */
  implicit def request: HttpServletRequest

  /**
   * A map of the current parameters.  The map contains the head of every 
   * non-empty value in `multiParams`.
   */
  def params: Map[String, String]

  /**
   * A multi-map of the current parameters.  Parameters may come from:
   * - the query string
   * - the POST body
   * - the route matchers of the currently executing route
   *
   * The map has a default value of `Seq.empty`.
   */
  def multiParams: MultiParams

  /**
   * The current response.
   */
  implicit def response: HttpServletResponse

  /**
   * Gets the content type of the current response.
   */
  def contentType = response.getContentType

  /**
   * Sets the content type of the current response.
   */
  def contentType_=(contentType: String) = response.setContentType(contentType)

  /**
   * Sets the status code of the current response.
   */
  def status(code: Int) = response.setStatus(code)

  /**
   * Sends a redirect response.
   */
  def redirect(uri: String) = response.sendRedirect(uri)

  /**
   * The current HTTP session.  Creates a session if none exists.
   */
  implicit def session: HttpSession = request.getSession

  /** 
   * The current HTTP session.  If none exists, None is returned.
   */
  def sessionOption: Option[HttpSession] = Option(request.getSession(false))

  /**
   * Adds a filter to run before the route.  
   *
   * Equivalent to beforeSome()(block).
   */
  final def beforeAll(block: => Any): Unit = beforeSome()(block)

  @deprecated("Use beforeAll", "2.0")
  final def before(block: => Any): Unit = beforeAll(block)

  /**
   * Adds a filter to run before the route.  The filter only runs if each
   * routeMatcher returns Some.
   */
  def beforeSome(routeMatchers: RouteMatcher*)(block: => Any): Unit

  /**
   * Adds a filter to run after the route.  
   *
   * Equivalent to afterSome()(block).
   */
  final def afterAll(block: => Any): Unit = afterSome()(block)

  @deprecated("Use afterAll", "2.0")
  final def after(block: => Any): Unit = afterAll(block)

  /**
   * Adds a filter to run after the route.  The filter only runs if each
   * routeMatcher returns Some.
   */
  def afterSome(routeMatchers: RouteMatcher*)(block: => Any): Unit

  /**
   * Defines a block to run if no matching routes are found, or if all
   * matching routes pass.
   */
  def notFound(block: => Any): Unit

  /**
   * Defines a block to run if matching routes are found only for other 
   * methods.  The set of matching methods is passed to the block.
   */
  def methodNotAllowed(block: Set[HttpMethod] => Any): Unit

  /**
   * Defines a block to run if an exception is thrown.  The exception is
   * passed to the block.
   */
  def error(block: => Any): Unit

  /**
   * The throwable currently handled by the error block.  Invalid outside
   * an error block.
   */
  def caughtThrowable: Throwable

  /**
   * The Scalatra DSL core methods take a list of [[org.scalatra.RouteMatcher]]
   * and a block as the action body.  The return value of the block is 
   * rendered through the pipeline and sent to the client as the response body.
   *
   * See [[org.scalatra.ScalatraKernel.renderResponseBody]] for the detailed 
   * behaviour and how to handle your response body more explicitly, and see 
   * how different return types are handled.
   *
   * The block is executed in the context of a CoreDsl instance, so all the 
   * methods defined in this trait are also available inside the block.
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
   * ScalatraKernel provides implicit transformation from boolean blocks, 
   * strings and regular expressions to [[org.scalatra.RouteMatcher]], so 
   * you can write code naturally.
   * {{{
   *   get("/", request.getRemoteHost == "127.0.0.1") { "Hello localhost!" }
   * }}}
   *
   */
  def get(routeMatchers: RouteMatcher*)(block: => Any): Route

  /**
   * @see get
   */
  def post(routeMatchers: RouteMatcher*)(block: => Any): Route

  /**
   * @see get
   */
  def put(routeMatchers: RouteMatcher*)(block: => Any): Route

  /**
   * @see get
   */
  def delete(routeMatchers: RouteMatcher*)(block: => Any): Route

  /**
   * @see get
   */
  def options(routeMatchers: RouteMatcher*)(block: => Any): Route

  /**
   * @see patch
   */
  def patch(routeMatchers: RouteMatcher*)(block: => Any): Route

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
           reason: String = null): Nothing

  /**
   * Immediately passes execution to the next matching route.
   */
  def pass()
}
