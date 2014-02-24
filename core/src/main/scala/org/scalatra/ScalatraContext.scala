package org.scalatra

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import servlet.ServletApiImplicits
import util.{MapWithIndifferentAccess, MultiMapHeadView}
import javax.servlet.ServletContext
import annotation.implicitNotFound

class ScalatraParams(protected val multiMap: Map[String, Seq[String]]) extends MultiMapHeadView[String, String] with MapWithIndifferentAccess[String]

object ScalatraContext {
  private class StableValuesContext(implicit val request: HttpServletRequest, val response: HttpServletResponse, val servletContext: ServletContext) extends ScalatraContext
}

trait ScalatraContext extends ServletApiImplicits with SessionSupport with CookieContext {
  import ScalatraContext.StableValuesContext
  def request: HttpServletRequest
  def response: HttpServletResponse
  def servletContext: ServletContext

  /**
   * Gets the content type of the current response.
   */
  def contentType(implicit response: HttpServletResponse): String = response.contentType getOrElse null

  /**
   * Gets the status code of the current response.
   */
  def status(implicit response: HttpServletResponse): Int = response.status.code


  /**
   * Sets the content type of the current response.
   */
  def contentType_=(contentType: String)(implicit response: HttpServletResponse) {
    response.contentType = Option(contentType)
  }

//  @deprecated("Use status_=(Int) instead", "2.1.0")
//  def status(code: Int) { status_=(code) }

  /**
   * Sets the status code of the current response.
   */
  def status_=(code: Int)(implicit response: HttpServletResponse) { response.status = ResponseStatus(code) }

//  /**
//   * Explicitly sets the request-scoped format.  This takes precedence over
//   * whatever was inferred from the request.
//   */
//  def format_=(formatValue: Symbol)(implicit request: HttpServletRequest) {
//    request(ApiFormats.FormatKey) = formatValue.name
//  }
//  TODO: fix this
//  /**
//   * Explicitly sets the request-scoped format.  This takes precedence over
//   * whatever was inferred from the request.
//   */
//  def format_=(formatValue: String)(implicit request: HttpServletRequest) {
//    request(ApiFormats.FormatKey) = formatValue
//  }

  protected[this] implicit def scalatraContext(implicit request: HttpServletRequest,
                                               response: HttpServletResponse): ScalatraContext  =
                  new StableValuesContext()(request, response, servletContext)
}