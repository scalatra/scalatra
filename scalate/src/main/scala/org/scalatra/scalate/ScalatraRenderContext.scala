package org.scalatra
package scalate

import servlet.ServletBase

import java.io.PrintWriter
import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpSession}
import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.servlet.ServletRenderContext

/**
 * A render context integrated with Scalatra.  Exposes a few extra
 * standard bindings to the template.
 */
class ScalatraRenderContext(
    protected val kernel: ServletBase,
    engine: TemplateEngine,
    out: PrintWriter,
    request: HttpServletRequest,
    response: HttpServletResponse)
  extends ServletRenderContext(engine, out, request, response, kernel.servletContext)
{
  def this(scalate: ScalateSupport, request: HttpServletRequest, response: HttpServletResponse) = this(scalate, scalate.templateEngine, response.getWriter, request, response)

  def this(scalate: ScalateSupport) = this(scalate, scalate.request, scalate.response)

  def flash: scala.collection.Map[String, Any] = kernel match {
    case flashMapSupport: FlashMapSupport => flashMapSupport.flash
    case _ => Map.empty
  }

  def session: HttpSession = kernel.session

  def sessionOption: Option[HttpSession] = kernel.sessionOption

  def params: Map[String, String] = kernel.params

  def multiParams: MultiParams = kernel.multiParams

  def csrfKey = kernel match {
    case csrfTokenSupport: CsrfTokenSupport => csrfTokenSupport.csrfKey
    case _ => ""
  }

  def csrfToken = kernel match {
    case csrfTokenSupport: CsrfTokenSupport => csrfTokenSupport.csrfToken
    case _ => ""
  }
}
