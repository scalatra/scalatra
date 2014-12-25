package org.scalatra
package servlet

import java.{ util => ju }
import javax.servlet.ServletContext
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import scala.collection.JavaConverters._
import scala.collection.immutable.DefaultMap

/**
 * ServletBase implements the Scalatra DSL with the Servlet API, and can be
 * a base trait of a Servlet or a Filter.
 */
trait ServletBase
    extends ScalatraBase
    with SessionSupport
    with Initializable {

  type ConfigT <: {

    def getServletContext(): ServletContext

    def getInitParameter(name: String): String

    def getInitParameterNames(): ju.Enumeration[String]

  }

  protected implicit def configWrapper(config: ConfigT) = new Config {

    override def context: ServletContext = config.getServletContext

    object initParameters extends DefaultMap[String, String] {

      override def get(key: String): Option[String] = Option(config.getInitParameter(key))

      override def iterator: Iterator[(String, String)] = {
        for (name <- config.getInitParameterNames.asScala.toIterator)
          yield (name, config.getInitParameter(name))
      }
    }

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
