package org.scalatra
package scalate

import java.io.PrintWriter
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse, HttpSession }

import org.fusesource.scalate.TemplateEngine
import org.fusesource.scalate.servlet.ServletRenderContext
import org.scalatra.servlet.{ FileItem, FileMultiParams, FileUploadSupport, ServletBase }

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
    case af: ApiFormats => af.format(request, response)
    case _ => ""
  }

  @deprecated("`format` now means the same as `responseFormat`, `responseFormat` will be removed eventually", "2.3")
  def responseFormat: String = format

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

  /**
   * Calculate a URL for a reversible route and some params.
   *
   * @param route a reversible route
   * @param params a list of named param/value pairs
   * @return a URI that matches the route for the given params
   * @throws Exception if the route is not reversible
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(route: Route, params: (String, String)*): String = UrlGenerator.url(route, params: _*)(request)

  /**
   * Calculate a URL for a reversible route and some splats.
   *
   * @param route a reversible route
   * @param splat the first splat parameter
   * @param moreSplats any splat parameters beyond the first
   * @return a URI that matches the route for the given splats
   * @throws Exception if the route is not reversible
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(route: Route, splat: String, moreSplats: String*): String = UrlGenerator.url(route, splat, moreSplats: _*)(request)

  /**
   * Calculate a URL for a reversible route, some params, and some splats.
   *
   * @param route a reversible route
   * @param params a map of param/value pairs
   * @param splats a series of splat parameters
   * @return a URI that matches the route for the given splats
   * @throws Exception if the route is not reversible
   * @throws IllegalStateException if the route's base path cannot be
   * determined.  This may occur outside of an HTTP request's lifecycle.
   */
  def url(
    route: Route,
    params: Map[String, String],
    splats: Iterable[String]): String = UrlGenerator.url(route, params, splats)(request)
}
