package org.scalatra

import javax.servlet._
import javax.servlet.http._
import scala.collection.JavaConversions._
import scala.collection.immutable.DefaultMap
import util.{MultiMap, MapWithIndifferentAccess, MultiMapHeadView, using}
import java.{util => ju}

object ScalatraKernel
{
  type MultiParams = MultiMap

  type Action = () => Any

  @deprecated("Use HttpMethods.methods")
  val httpMethods = HttpMethod.methods map { _.toString }

  @deprecated("Use HttpMethods.methods filter { !_.isSafe }")
  val writeMethods = HttpMethod.methods filter { !_.isSafe } map { _.toString }

  @deprecated("Use CsrfTokenSupport.DefaultKey")
  val csrfKey = CsrfTokenSupport.DefaultKey

  val EnvironmentKey = "org.scalatra.environment".intern

  val MultiParamsKey = "org.scalatra.MultiParams".intern
}
import ScalatraKernel._

/**
 * ScalatraKernel is the default implementation of [[org.scalatra.CoreDSL]].
 * It is typically extended by [[org.scalatra.ScalatraServlet]] or
 * [[org.scalatra.ScalatraFilter]] to create a Scalatra application.
 */
trait ScalatraKernel extends ScalatraService with Handler {
  def handle(req: HttpServletRequest, res: HttpServletResponse) {
    apply(req, res)
  }

  def session = request.getSession

  def sessionOption = Option(request.getSession(false))

  override def rewriteUriForSessionTracking(uri: String) = 
    response.encodeURL(uri)

  type Config <: {
    def getServletContext(): ServletContext
    def getInitParameter(name: String): String
    def getInitParameterNames(): ju.Enumeration[_]
  }

  protected implicit def configWrapper(config: Config) = new RichConfig {
    def context = config.getServletContext

    object initParameters extends DefaultMap[String, String] {
      def get(key: String): Option[String] =
	Option(config.getInitParameter(key))

      def iterator: Iterator[(String, String)] =
	for (name <- config.getInitParameterNames.asInstanceOf[ju.Enumeration[String]].toIterator) 
	  yield (name, config.getInitParameter(name))
    }
  }
}
