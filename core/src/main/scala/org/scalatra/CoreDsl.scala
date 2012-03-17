package org.scalatra

import scala.util.DynamicVariable

/**
 * The core Scalatra DSL.
 */
trait CoreDsl extends Handler with Control {
  /**
   * The type of application context supported by this application.  Made
   * abstract for compatibility with the servlet implementation.
   */
  type ApplicationContextT <: ApplicationContext

  /**
   * The application context.  It is shared among all handlers within this
   * application.
   */
  implicit def applicationContext: ApplicationContextT

  @deprecated("Use applicationContext instead", "2.1.0")
  def servletContext: ApplicationContextT = applicationContext

  /**
   * The current request
   */
  implicit def request: RequestT

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
  implicit def response: ResponseT

  /**
   * Gets the content type of the current response.
   */
  def contentType: String = response.contentType getOrElse null

  /**
   * Sets the content type of the current response.
   */
  def contentType_=(contentType: String): Unit = 
    response.contentType = Option(contentType)

  @deprecated("Use status_=(Int) instead", "2.1.0")
  def status(code: Int) = status_=(code)

  /**
   * Sets the status code of the current response.
   */
  def status_=(code: Int): Unit = response.status = ResponseStatus(code)

  /**
   * Gets the status code of the current response.
   */
  def status: Int = response.status.code

  /**
   * Sends a redirect response and immediately halts the current action.
   */
  def redirect(uri: String): Unit = {
    response.redirect(uri)
    halt()
  }

  /**
   * Adds a filter to run before the route.  The filter only runs if each
   * routeMatcher returns Some.  If the routeMatchers list is empty, the
   * filter runs for all routes.
   */
  def before(transformers: RouteTransformer*)(block: => Any): Unit

  @deprecated("Use before() { ... }", "2.0.0")
  final def beforeAll(block: => Any): Unit = before()(block)

  @deprecated("Use before(RouteTransformer*) { ... }", "2.0.0")
  final def beforeSome(transformers: RouteTransformer*)(block: => Any): Unit =
    before(transformers: _*)(block)

  /**
   * Adds a filter to run after the route.  The filter only runs if each
   * routeMatcher returns Some.  If the routeMatchers list is empty, the
   * filter runs for all routes.
   */
  def after(transformers: RouteTransformer*)(block: => Any): Unit

  @deprecated("Use after() { ... }", "2.0.0")
  final def afterAll(block: => Any): Unit = after()(block)

  @deprecated("Use after(RouteTransformer*) { ... }", "2.0.0")
  final def afterSome(transformers: RouteTransformer*)(block: => Any): Unit =
    before(transformers: _*)(block)

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
   * Defines an error handler for exceptions thrown in either the before
   * block or a route action.
   *
   * If the error handler does not match, the result falls through to the
   * previously defined error handler.  The default error handler simply
   * rethrows the exception.
   *
   * The error handler is run before the after filters, and the result is
   * rendered like a standard response.  It is the error handler's
   * responsibility to set any appropriate status code.
   */
  def error(handler: ErrorHandler): Unit

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
  def get(transformers: RouteTransformer*)(block: => Any): Route

  /**
   * @see get
   */
  def post(transformers: RouteTransformer*)(block: => Any): Route

  /**
   * @see get
   */
  def put(transformers: RouteTransformer*)(block: => Any): Route

  /**
   * @see get
   */
  def delete(transformers: RouteTransformer*)(block: => Any): Route

  /**
   * @see get
   */
  def options(transformers: RouteTransformer*)(block: => Any): Route

  /**
   * @see patch
   */
  def patch(transformers: RouteTransformer*)(block: => Any): Route

  /**
   * Error handler for HTTP response status code range. You can intercept every response code previously
   * specified with #status or even generic 404 error.
   * {{{
   *   trap(403) {
   *    "You are not authorized"
   *   }
   }* }}}
   }}*/
  def trap(codes: Range)(block: => Any): Unit

  /**
   * @see error
   */
  def trap(code: Int)(block: => Any): Unit = 
    trap(Range(code, code+1))(block)
}
