package org.scalatra
package servlet

import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}
import java.{util => ju}
import scala.collection.immutable.DefaultMap
import scala.collection.JavaConversions._

trait ServletDsl extends CoreDsl with ServletHandler with Initializable {
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
}
