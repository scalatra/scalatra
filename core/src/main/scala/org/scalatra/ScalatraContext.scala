package org.scalatra

import _root_.akka.util.Timeout
import servlet.ServletApiImplicits
import javax.servlet.ServletContext
import java.{ util => ju }
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import util.{MapWithIndifferentAccess, MultiMapHeadView}
import org.scalatra.util.conversion.DefaultImplicitConversions
import scala.util.control.Exception._
import util.RicherString._

trait ScalatraContext extends ServletApiImplicits with ScalatraParamsImplicits with DefaultImplicitConversions with SessionSupport with CookieContext with Control {
  import ScalatraBase._

  type ConfigT
  implicit def request: HttpServletRequest
  implicit def response: HttpServletResponse
  def config: ConfigT

  /**
   * Gets an init paramter from the config.
   *
   * @param name the name of the key
   *
   * @return an option containing the value of the parameter if defined, or
   * `None` if the parameter is not set.
   */
  def initParameter(name: String): Option[String] = servletContext.initParameters.get(name)

  /**
   * The servlet context in which this kernel runs.
   */
  def servletContext: ServletContext

  /**
   * A free form string representing the environment.
   * `org.scalatra.Environment` is looked up as a system property, and if
   * absent, and init parameter.  The default value is `development`.
   */
  def environment: String = System.getProperty(EnvironmentKey, initParameter(EnvironmentKey).getOrElse("DEVELOPMENT"))

  /**
   * A boolean flag representing whether the kernel is in development mode.
   * The default is true if the `environment` begins with "dev", case-insensitive.
   */
  def isDevelopmentMode = environment.toUpperCase.startsWith("DEV")

  /**
   * The current multiparams.  Multiparams are a result of merging the
   * standard request params (query string or post params) with the route
   * parameters extracted from the route matchers of the current route.
   * The default value for an unknown param is the empty sequence.  Invalid
   * outside `handle`.
   */
  def multiParams: MultiParams = {
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
  protected val _params: Params = new MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] {
    protected def multiMap = multiParams
  }

  def params: Params = _params

  /**
   * The effective path against which routes are matched.  The definition
   * varies between servlets and filters.
   */
  def requestPath: String

  def relativeUrl(path: String, params: Iterable[(String, Any)] = Iterable.empty, includeContextPath: Boolean = true, includeServletPath: Boolean = true): String = {
    url(path, params, includeContextPath, includeServletPath, absolutize = false)
  }

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
  def url(path: String, params: Iterable[(String, Any)] = Iterable.empty, includeContextPath: Boolean = true, includeServletPath: Boolean = true, absolutize: Boolean = true): String = {

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
      case (key, None) => key.urlEncode +"="
      case (key, Some(value)) => key.urlEncode + "=" + value.toString.urlEncode
      case(key, value) => key.urlEncode + "=" +value.toString.urlEncode
    }
    val queryString = if (pairs.isEmpty) "" else pairs.mkString("?", "&", "")
    addSessionId(newPath + queryString)
  }

  private[this] val ensureContextPathsStripped = (ensureContextPathStripped _) andThen (ensureServletPathStripped _)

  private[this] def ensureServletPathStripped(path: String) = {
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
    val p = if (candidate.startsWith("/")) candidate else "/"+candidate
    if (p.endsWith("/")) p.dropRight(1) else p
  }


  protected def isHttps = { // also respect load balancer version of the protocol
    val h = request.getHeader("X-Forwarded-Proto").blankOption
    request.isSecure || (h.isDefined && h.forall(_ equalsIgnoreCase "HTTPS"))
  }

  protected def needsHttps =
    allCatch.withApply(_ => false) {
      servletContext.getInitParameter(ForceHttpsKey).blankOption.map(_.toBoolean) getOrElse false
    }

  /**
   * Gets the content type of the current response.
   */
  def contentType: String = response.contentType getOrElse null

  /**
   * Sets the content type of the current response.
   */
  def contentType_=(contentType: String) {
    response.contentType = Option(contentType)
  }

  @deprecated("Use status_=(Int) instead", "2.1.0")
  def status(code: Int) { status_=(code) }

  /**
   * Sets the status code of the current response.
   */
  def status_=(code: Int) { response.status = ResponseStatus(code) }

  /**
   * Gets the status code of the current response.
   */
  def status: Int = response.status.code


  /**
   * Sends a redirect response and immediately halts the current action.
   */
  def redirect(uri: String) {
    response.redirect(fullUrl(uri, includeServletPath = false))
    halt()
  }

  /**
   * The base path for URL generation
   */
  protected def routeBasePath: String

  /**
   * Builds a full URL from the given relative path. Takes into account the port configuration, https, ...
   *
   * @param path a relative path
   *
   * @return the full URL
   */
  def fullUrl(path: String, params: Iterable[(String, Any)] = Iterable.empty, includeContextPath: Boolean = true, includeServletPath: Boolean = true) = {
    if (path.startsWith("http")) path
    else {
      val p = url(path, params, includeContextPath, includeServletPath)
      buildBaseUrl + ensureSlash(p)
    }
  }

  private[this] def buildBaseUrl = {
    "%s://%s".format(
      if (needsHttps || isHttps) "https" else "http",
      serverAuthority
    )
  }

  private[this] def serverAuthority = {
    val p = serverPort
    val h = serverHost
    if (p == 80 || p == 443 ) h else h+":"+p.toString
  }

  def serverHost = {
    servletContext.getInitParameter(HostNameKey).blankOption getOrElse request.getServerName
  }

  def serverPort = {
    servletContext.getInitParameter(PortKey).blankOption.map(_.toInt) getOrElse request.getServerPort
  }

  protected def contextPath: String = servletContext.contextPath

  protected def addSessionId(uri: String): String = response.encodeURL(uri)
}