package org.scalatra
package servlet

import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse, HttpSession}
import java.{util => ju}
import java.util.Locale
import scala.collection.immutable.DefaultMap
import scala.collection.JavaConversions._

/**
 * ServletBase implements the Scalatra DSL with the Servlet API, and can be
 * a base trait of a Servlet or a Filter.
 */
trait ServletBase 
  extends ScalatraBase
  with ServletHandler
  with SessionSupport 
  with Initializable
{
  type SessionT = ServletSession
  type ApplicationContextT = ServletApplicationContext
  type ConfigT <: {
    def getServletContext(): ServletContext
    def getInitParameter(name: String): String
    def getInitParameterNames(): ju.Enumeration[String]
  }

  protected implicit def configWrapper(config: ConfigT) = new Config {
    def context = ServletApplicationContext(config.getServletContext)

    object initParameters extends DefaultMap[String, String] {
      def get(key: String): Option[String] =
	Option(config.getInitParameter(key))

      def iterator: Iterator[(String, String)] =
	for (name <- config.getInitParameterNames.toIterator) 
	  yield (name, config.getInitParameter(name))
    }
  }

  override implicit def session: SessionT = ServletSession(request.getSession)

  override implicit def sessionOption: Option[SessionT] =
    Option(request.getSession(false)) map ServletSession.apply

  override def addSessionId(uri: String) = response.encodeURL(uri)

  protected def requestWithMethod(req: RequestT, m: HttpMethod) =
    new ServletRequest(req) {
      override def getMethod = m.toString.toUpperCase(Locale.ENGLISH)
    }

  override def handle(request: RequestT, response: ResponseT) {
    // As default, the servlet tries to decode params with ISO_8859-1.
    // It causes an EOFException if params are actually encoded with the 
    // other code (such as UTF-8)
    if (request.getCharacterEncoding == null)
      request.setCharacterEncoding(defaultCharacterEncoding)
    super.handle(request, response)
  }

  @deprecated("Use handle(ServletRequest, ServletResponse)", "2.1.0")
  def handle(req: HttpServletRequest, res: HttpServletResponse) {
    handle(ServletRequest(req), ServletResponse(res))
  }

  /**
   * For compatibility.  Ensures that the request is wrapped in Scalatra's
   * request abstraction.
   */
  @deprecated("Remove references to HttpServletRequest from Scalatra apps", "2.1.0")
  protected implicit def ensureServletRequest(request: HttpServletRequest): ServletRequest =
    request match {
      case r: ServletRequest => r
      case _ => ServletRequest(request)
    }

  /**
   * For compatibility.  Ensures that the request is wrapped in Scalatra's
   * response abstraction.
   */
  @deprecated("Remove references to HttpServletResponse from Scalatra apps", "2.1.0")
  protected implicit def ensureServletResponse(response: HttpServletResponse): ServletResponse =
    request match {
      case r: ServletResponse => r
      case _ => ServletResponse(response)
    }
}
