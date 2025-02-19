package org.scalatra
package servlet

import java.util as ju
import org.scalatra.ServletCompat.ServletContext
import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpServletResponse}
import scala.language.reflectiveCalls

/** ServletBase implements the Scalatra DSL with the Servlet API, and can be a base trait of a Servlet or a Filter.
  */
trait ServletBase extends ScalatraBase with SessionSupport with Initializable {

  type ConfigT <: {

    def getServletContext(): ServletContext

    def getInitParameter(name: String): String

    def getInitParameterNames(): ju.Enumeration[String]

  }

  protected implicit def configWrapper(config: ConfigT): Config = new Config {

    override def context: ServletContext = config.getServletContext()

    def getInitParameterOption(key: String): Option[String] = Option(config.getInitParameter(key))

  }

  override def handle(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    // As default, the servlet tries to decode params with ISO_8859-1.
    // It causes an EOFException if params are actually encoded with the
    // other code (such as UTF-8)
    if (request.getCharacterEncoding == null) {
      request.setCharacterEncoding(defaultCharacterEncoding)
    }
    super.handle(request, response)
  }

}
