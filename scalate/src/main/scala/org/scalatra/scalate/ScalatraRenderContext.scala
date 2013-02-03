package org.scalatra
package scalate

import servlet.{FileItem, FileUploadSupport, FileMultiParams, ServletBase}

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
    req: HttpServletRequest,
    res: HttpServletResponse) extends ServletRenderContext(engine, out, req, res, kernel.servletContext) {

  def flash: scala.collection.Map[String, Any] = kernel match {
    case flashMapSupport: FlashMapSupport => flashMapSupport.flash(request)
    case _ => Map.empty
  }

  def session: HttpSession = kernel.session(request)

  def sessionOption: Option[HttpSession] = kernel.sessionOption(request)

  def params: Params = kernel.params(request)

  def multiParams: MultiParams = kernel.multiParams(request)

  def format: String = kernel match {
    case af: ApiFormats => af.format(request)
    case _ => ""
  }

  def responseFormat: String = kernel match {
    case af: ApiFormats => af.responseFormat(request, response)
    case _ => ""
  }

  def fileMultiParams: FileMultiParams = kernel match {
    case fu: FileUploadSupport => fu.fileMultiParams(request)
    case _ => new FileMultiParams()
  }

  def fileParams: scala.collection.Map[String, FileItem] = kernel match {
    case fu: FileUploadSupport => fu.fileParams(request)
    case _ => Map.empty
  }

  def csrfKey = kernel match {
    case csrfTokenSupport: CsrfTokenSupport => csrfTokenSupport.csrfKey
    case _ => ""
  }

  def csrfToken = kernel match {
    case csrfTokenSupport: CsrfTokenSupport => csrfTokenSupport.csrfToken(request)
    case _ => ""
  }
  def xsrfKey = kernel match {
    case csrfTokenSupport: XsrfTokenSupport => csrfTokenSupport.xsrfKey
    case _ => ""
  }

  def xsrfToken = kernel match {
    case csrfTokenSupport: XsrfTokenSupport => csrfTokenSupport.xsrfToken(request)
    case _ => ""
  }
}
