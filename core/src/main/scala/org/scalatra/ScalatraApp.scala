package org.scalatra

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.util.concurrent.atomic.AtomicReference
import collection.immutable.DefaultMap
import javax.servlet.ServletContext
import java.{ util => ju }
import collection.JavaConverters._

abstract class ScalatraApp[T <: {
    def getServletContext(): ServletContext
    def getInitParameter(name: String): String
    def getInitParameterNames(): ju.Enumeration[String]
  }](override val config: T, req: HttpServletRequest, res: HttpServletResponse) extends ScalatraSyntax with RequestResponseScope with Initializable {

  type ConfigT = T
  private[this] val _request = new AtomicReference[HttpServletRequest](req)
  private[this] val _response = new AtomicReference[HttpServletResponse](res)

  protected def withRequestResponse[A](request: HttpServletRequest, response: HttpServletResponse)(f: => A): A = {
    val oldReq = this.request
    val oldRes = this.response
    val resetReq = _request.compareAndSet(oldReq, request)
    val resetRes = _response.compareAndSet(oldRes, response)
    val res = f
    if (resetRes) _response.compareAndSet(response, oldRes)
    if (resetReq) _request.compareAndSet(request, oldReq)
    res
  }

  /**
   * Executes the block with the given request bound to the `request`
   * method.
   */
  protected def withRequest[A](request: HttpServletRequest)(f: => A): A = {
    val oldReq = this.request
    val reset = _request.compareAndSet(oldReq, request)
    val res = f
    if (reset) _request.compareAndSet(request, oldReq)
    res
  }

  /**
   * Executes the block with the given response bound to the `response`
   * method.
   */
  protected def withResponse[A](response: HttpServletResponse)(f: => A): A = {
    val oldRes = this.response
    _response.set(response)
    val res = f
    _response.set(oldRes)
    res
  }


  /**
   * The currently scoped request.  Valid only inside the `handle` method.
   */
  implicit def request: HttpServletRequest = _request.get()

  /**
   * The currently scoped response.  Valid only inside the `handle` method.
   */
  implicit def response: HttpServletResponse = _response.get()

  @deprecated("Move this code into the constructor, the initialize method is not really used in a scalatra app.", "2.2")
  def initialize(config: ConfigT) {}
  initialize(config)

  /**
   * Defines the request path to be matched by routers.  The default
   * definition is optimized for `path mapped` servlets (i.e., servlet
   * mapping ends in `&#47;*`).  The route should match everything matched by
   * the `&#47;*`.  In the event that the request URI equals the servlet path
   * with no trailing slash (e.g., mapping = `/admin&#47;*`, request URI =
   * '/admin'), a '/' is returned.
   *
   * All other servlet mappings likely want to return request.getServletPath.
   * Custom implementations are allowed for unusual cases.
   */
  def requestPath = {
    def getRequestPath = request.getRequestURI match {
      case requestURI: String =>
        var uri = requestURI
        if (request.getContextPath != null && request.getContextPath.trim.nonEmpty) uri = uri.substring(request.getContextPath.length)
        if (request.getServletPath != null && request.getServletPath.trim.nonEmpty) uri = uri.substring(request.getServletPath.length)
        if (uri.isEmpty) {
          uri = "/"
        } else {
          val pos = uri.indexOf(';')
          if (pos >= 0) uri = uri.substring(0, pos)
        }
        UriDecoder.firstStep(uri)
      case null => "/"
    }

    request.get("org.scalatra.ScalatraServlet.requestPath") match {
      case Some(uri) => uri.toString
      case _         => {
        val requestPath = getRequestPath
        request.setAttribute("org.scalatra.ScalatraServlet.requestPath", requestPath)
        requestPath.toString
      }
    }
  }

  protected def routeBasePath = {
    if (request == null)
      throw new IllegalStateException("routeBasePath requires an active request to determine the servlet path")
    request.getContextPath + request.getServletPath
  }

  /**
   * Invoked when no route matches.  By default, calls `serveStaticResource()`,
   * and if that fails, calls `resourceNotFound()`.
   *
   * This action can be overridden by a notFound block.
   */
  protected var doNotFound: Action = () => {
    serveStaticResource() getOrElse resourceNotFound()
  }

  /**
   * Attempts to find a static resource matching the request path.  Override
   * to return None to stop this.
   */
  protected def serveStaticResource(): Option[Any] =
    servletContext.resource(request) map { _ =>
      servletContext.getNamedDispatcher("default").forward(request, response)
    }

  /**
   * Called by default notFound if no routes matched and no static resource
   * could be found.
   */
  protected def resourceNotFound(): Any = {
    response.setStatus(404)
    if (isDevelopmentMode) {
      val error = "Requesting \"%s %s\" on servlet \"%s\" but only have: %s"
      response.getWriter println error.format(
        request.getMethod,
        Option(request.getPathInfo) getOrElse "/",
        request.getServletPath,
        routes.entryPoints.mkString("<ul><li>", "</li><li>", "</li></ul>"))
    }
  }

  protected implicit def configWrapper(config: ConfigT) = new Config {
    def context = config.getServletContext

    object initParameters extends DefaultMap[String, String] {
      def get(key: String): Option[String] = Option(config.getInitParameter(key))

      def iterator: Iterator[(String, String)] = new Iterator[(String, String)] {
        private val b = config.getInitParameterNames()
        def hasNext: Boolean =  b.hasMoreElements

        def next(): (String, String) = {
          val name = b.nextElement()
          (name, config.getInitParameter(name))
        }
      }
    }
  }


    def addSessionId(uri: String) = response.encodeURL(uri)

    override def handle(request: HttpServletRequest, response: HttpServletResponse) {
      // As default, the servlet tries to decode params with ISO_8859-1.
      // It causes an EOFException if params are actually encoded with the
      // other code (such as UTF-8)
      if (request.getCharacterEncoding == null)
        request.setCharacterEncoding(defaultCharacterEncoding)
      super.handle(request, response)
    }
}

