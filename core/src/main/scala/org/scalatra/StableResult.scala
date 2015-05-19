package org.scalatra

import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

// Provides a stable request/response to an action.
// Each action is wrapped in a StableResult during compilation.
abstract class StableResult(
  implicit override val scalatraContext: ScalatraContext)
    extends ScalatraContext {

  implicit val request: HttpServletRequest = scalatraContext.request

  implicit val response: HttpServletResponse = scalatraContext.response

  val servletContext: ServletContext = scalatraContext.servletContext

  val is: Any

}
