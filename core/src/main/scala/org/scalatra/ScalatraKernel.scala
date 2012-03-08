package org.scalatra

import servlet.ServletDsl
import util.MultiMap

object ScalatraKernel
{
  @deprecated("Use org.scalatra.MultiParams") // since 2.1
  type MultiParams = MultiMap

  @deprecated("Use org.scalatra.Action") // since 2.1
  type Action = () => Any

  @deprecated("Use HttpMethod.methods")
  val httpMethods = HttpMethod.methods map { _.toString }

  @deprecated("Use HttpMethod.methods filter { !_.isSafe }")
  val writeMethods = HttpMethod.methods filter { !_.isSafe } map { _.toString }

  @deprecated("Use CsrfTokenSupport.DefaultKey")
  val csrfKey = CsrfTokenSupport.DefaultKey

  @deprecated("Use org.scalatra.EnvironmentKey") // since 2.1
  val EnvironmentKey = "org.scalatra.environment".intern

  @deprecated("Use org.scalatra.MultiParamsKey") // since 2.1
  val MultiParamsKey = "org.scalatra.MultiParams".intern
}
import ScalatraKernel._

/**
 * ScalatraKernel is the default implementation of [[org.scalatra.CoreDSL]].
 * It is typically extended by [[org.scalatra.ScalatraServlet]] or
 * [[org.scalatra.ScalatraFilter]] to create a Scalatra application.
 */
trait ScalatraKernel extends ScalatraBase with ServletDsl {
  override def addSessionId(uri: String) = response.encodeUrl(uri)
}
