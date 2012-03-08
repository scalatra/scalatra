package org.scalatra
package servlet

import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}
import java.{util => ju}
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
  type SessionT = HttpSession
  type ApplicationContextT = ServletContext
  type ConfigT <: {
    def getServletContext(): ServletContext
    def getInitParameter(name: String): String
    def getInitParameterNames(): ju.Enumeration[String]
  }

  protected implicit def configWrapper(config: ConfigT) = new Config {
    def context = config.getServletContext

    object initParameters extends DefaultMap[String, String] {
      def get(key: String): Option[String] =
	Option(config.getInitParameter(key))

      def iterator: Iterator[(String, String)] =
	for (name <- config.getInitParameterNames.toIterator) 
	  yield (name, config.getInitParameter(name))
    }
  }

  override implicit def session: SessionT = request.getSession

  override implicit def sessionOption: Option[SessionT] =
    Option(request.getSession(false))

  override def addSessionId(uri: String) = response.encodeUrl(uri)
}
