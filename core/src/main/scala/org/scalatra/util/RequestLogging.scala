package org.scalatra
package util

import jakarta.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.util.RicherString._
import org.slf4j.{ LoggerFactory, MDC }

import scala.jdk.CollectionConverters._

object RequestLogging {

  val CgiParamsKey = "org.scalatra.slf4j.ScalatraSlf4jSupport"
  val RequestPath = "REQUEST_PATH"
  val RequestApp = "REQUEST_APP" // maps to a scalatra servlet
  val RequestParams = "REQUEST_PARAMS"
  val SessionParams = "SESSION_PARAMS"
  val CgiParams = "CGI_PARAMS"

}

/**
 * Logs request information using slf4j with "REQUEST" logger name
 * by mixing-in this trait to scalatra servlet or filter.
 */
trait RequestLogging extends ScalatraBase with Handler {

  private[this] val logger = LoggerFactory.getLogger("REQUEST")
  import org.scalatra.util.RequestLogging._

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    val realMultiParams = req.getParameterMap.asScala.toMap transform { (k, v) => v.toIndexedSeq }
    withRequest(req) {
      request(MultiParamsKey) = realMultiParams
      request(CgiParamsKey) = readCgiParams(req)
      fillMdc()
      super.handle(req, res)
      MDC.clear()
    }
  }

  protected def logRequest(): Unit = {
    logger.info(MDC.getCopyOfContextMap.asScala.map(kv => kv._1.toString + ": " + kv._2.toString).mkString("{", ", ", " }"))
  }

  override protected[scalatra] def withRouteMultiParams[S](matchedRoute: Option[MatchedRoute])(thunk: => S)(implicit request: HttpServletRequest): S = {
    val originalParams = multiParams
    request(MultiParamsKey) = originalParams ++ matchedRoute.map(_.multiParams).getOrElse(Map.empty)
    fillMdc()
    try { thunk } finally { request(MultiParamsKey) = originalParams }
  }

  private[this] def fillMdc(): Unit = { // Do this twice so that we get all the route params if they are available and applicable
    MDC.clear()
    MDC.put(RequestPath, requestPath)
    MDC.put(RequestApp, getClass.getSimpleName)
    MDC.put(RequestParams, multiParams map { case (k, vl) => vl.map(v => "%s=%s".format(%-(k), %-(v))) } mkString "&")
    MDC.put(SessionParams, session.dumpAll)
    MDC.put(CgiParams, cgiParams map { case (k, v) => "%s=%s".format(%-(k), %-(v)) } mkString "&")
  }

  private[this] def cgiParams = request get CgiParamsKey map (_.asInstanceOf[Map[String, String]]) getOrElse Map.empty

  private[this] def readCgiParams(req: HttpServletRequest) = Map(
    "AUTH_TYPE" -> req.getAuthType,
    "CONTENT_LENGTH" -> req.getContentLength.toString,
    "CONTENT_TYPE" -> req.getContentType,
    "DOCUMENT_ROOT" -> servletContext.getRealPath(servletContext.getContextPath),
    "PATH_INFO" -> req.getPathInfo,
    "PATH_TRANSLATED" -> req.getPathTranslated,
    "QUERY_STRING" -> req.getQueryString,
    "REMOTE_ADDR" -> req.getRemoteAddr,
    "REMOTE_HOST" -> req.getRemoteHost,
    "REMOTE_USER" -> req.getRemoteUser,
    "REQUEST_METHOD" -> req.getMethod,
    "SCRIPT_NAME" -> req.getServletPath,
    "SERVER_NAME" -> req.getServerName,
    "SERVER_PORT" -> req.getServerPort.toString,
    "SERVER_PROTOCOL" -> req.getProtocol,
    "SERVER_SOFTWARE" -> servletContext.getServerInfo)

  private def %-(s: String) = s.blankOption map (_.urlEncode) getOrElse ""

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
  override protected def addRoute(method: HttpMethod, transformers: Seq[_root_.org.scalatra.RouteTransformer], action: => Any): Route = {
    val newAction = () => {
      try { logRequest() } catch { case _: Throwable => }
      action
    }
    val route = Route(transformers, newAction, (req: HttpServletRequest) => routeBasePath(req))
    routes.prependRoute(method, route)
    route
  }
}

