package org.scalatra

object ScalatraKernel {

  @deprecated("Use org.scalatra.MultiParams", "2.1.0")
  type MultiParams = util.MultiMap

  @deprecated("Use org.scalatra.Action", "2.1.0")
  type Action = () => Any

  @deprecated("Use HttpMethod.methods", "2.0.0")
  val httpMethods = HttpMethod.methods map { _.toString }

  @deprecated("Use HttpMethod.methods filter { !_.isSafe }", "2.0.0")
  val writeMethods = HttpMethod.methods filter { !_.isSafe } map { _.toString }

  @deprecated("Use CsrfTokenSupport.DefaultKey", "2.0.0")
  val csrfKey = CsrfTokenSupport.DefaultKey

  @deprecated("Use org.scalatra.EnvironmentKey", "2.1.0")
  val EnvironmentKey = "org.scalatra.environment"

  @deprecated("Use org.scalatra.MultiParamsKey", "2.1.0")
  val MultiParamsKey = "org.scalatra.MultiParams"

}
