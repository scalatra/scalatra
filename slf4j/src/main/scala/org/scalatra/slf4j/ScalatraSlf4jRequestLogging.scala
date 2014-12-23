package org.scalatra
package slf4j

import java.util.{ Map => JMap }
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import grizzled.slf4j.Logger
import org.scalatra.util.MultiMap
import org.scalatra.util.RicherString._
import org.slf4j.MDC

import scala.collection.JavaConverters._

object ScalatraSlf4jRequestLogging {

  val CgiParamsKey = "org.scalatra.slf4j.ScalatraSlf4jSupport"
  val RequestPath = "REQUEST_PATH"
  val RequestApp = "REQUEST_APP" // maps to a scalatra servlet
  val RequestParams = "REQUEST_PARAMS"
  val SessionParams = "SESSION_PARAMS"
  val CgiParams = "CGI_PARAMS"

}

trait ScalatraSlf4jRequestLogging extends ScalatraBase with Handler {

  private[this] val logger = Logger("REQUEST")
  import org.scalatra.slf4j.ScalatraSlf4jRequestLogging._

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    val realMultiParams = req.getParameterMap.asScala.toMap transform { (k, v) ⇒ v: Seq[String] }
    withRequest(req) {
      request(MultiParamsKey) = MultiMap(Map() ++ realMultiParams)
      request(CgiParamsKey) = readCgiParams(req)
      fillMdc()
      super.handle(req, res)
      MDC.clear()
    }
  }

  protected def logRequest() {
    logger.info(MDC.getCopyOfContextMap.asScala.map(kv => kv._1.toString + ": " + kv._2.toString).mkString("{", ", ", " }"))
  }

  override protected def withRouteMultiParams[S](matchedRoute: Option[MatchedRoute])(thunk: ⇒ S): S = {
    val originalParams = multiParams
    request(MultiParamsKey) = originalParams ++ matchedRoute.map(_.multiParams).getOrElse(Map.empty)
    fillMdc()
    try { thunk } finally { request(MultiParamsKey) = originalParams }
  }

  private[this] def fillMdc() { // Do this twice so that we get all the route params if they are available and applicable
    MDC.clear()
    MDC.put(RequestPath, requestPath)
    MDC.put(RequestApp, getClass.getSimpleName)
    MDC.put(RequestParams, multiParams map { case (k, vl) ⇒ vl.map(v ⇒ "%s=%s".format(%-(k), %-(v))) } mkString "&")
    this match {
      case a: SessionSupport =>
        MDC.put(SessionParams, a.session map { case (k, v) ⇒ "%s=%s".format(%-(k), %-(v.toString)) } mkString "&")
      case _ =>
    }

    MDC.put(CgiParams, cgiParams map { case (k, v) ⇒ "%s=%s".format(%-(k), %-(v)) } mkString "&")
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

