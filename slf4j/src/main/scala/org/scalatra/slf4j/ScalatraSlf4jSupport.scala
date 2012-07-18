package org.scalatra
package slf4j

import org.scalatra.util.MultiMap
import org.slf4j.MDC
import java.util.{ Map ⇒ JMap }
import grizzled.slf4j.Logging
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import collection.JavaConverters._
import org.scalatra.util.RicherString._

object ScalatraLogbackSupport {

  val CgiParamsKey = "org.scalatra.slf4j.ScalatraSlf4jSupport"
  val RequestPath = "REQUEST_PATH"
  val RequestApp = "REQUEST_APP" // maps to a scalatra servlet
  val RequestParams = "REQUEST_PARAMS"
  val SessionParams = "SESSION_PARAMS"
  val CgiParams = "CGI_PARAMS"

}

trait ScalatraLogbackSupport extends Handler with Logging { self: ScalatraBase ⇒

  import ScalatraLogbackSupport._

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    val realMultiParams = req.getParameterMap.asInstanceOf[JMap[String, Array[String]]].asScala.toMap transform { (k, v) ⇒ v: Seq[String] }
    withRequest(req) {
      request(MultiParamsKey) = MultiMap(Map() ++ realMultiParams)
      request(CgiParamsKey) = readCgiParams(req)
      fillMdc()
      super.handle(req, res)
      MDC.clear()
    }
  }

  override protected def withRouteMultiParams[S](matchedRoute: Option[MatchedRoute])(thunk: ⇒ S): S = {
    val originalParams = multiParams
    request(MultiParamsKey) = originalParams ++ matchedRoute.map(_.multiParams).getOrElse(Map.empty)
    fillMdc()
    try { thunk } finally { request(MultiParamsKey) = originalParams }
  }

  protected def fillMdc() { // Do this twice so that we get all the route params if they are available and applicable
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

  def cgiParams = request get CgiParamsKey map (_.asInstanceOf[Map[String, String]]) getOrElse Map.empty

  private def readCgiParams(req: HttpServletRequest) = Map(
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
}

