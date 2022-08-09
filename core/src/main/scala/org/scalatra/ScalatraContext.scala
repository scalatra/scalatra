package org.scalatra

import jakarta.servlet.ServletContext
import jakarta.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.servlet.{ HttpServletRequestReadOnly, ServletApiImplicits }

object ScalatraContext {

  private class StableValuesContext(
    implicit
    val request: HttpServletRequest,
    val response: HttpServletResponse,
    val servletContext: ServletContext) extends ScalatraContext
}

trait ScalatraContext
  extends ServletApiImplicits
  with SessionSupport
  with CookieContext {

  import org.scalatra.ScalatraContext.StableValuesContext

  implicit def request: HttpServletRequest

  implicit def response: HttpServletResponse

  def servletContext: ServletContext

  /**
   * Gets the content type of the current response.
   */
  def contentType: String = response.contentType.orNull

  /**
   * Gets the status code of the current response.
   */
  def status: Int = response.status

  /**
   * Sets the content type of the current response.
   */
  def contentType_=(contentType: String): Unit = {
    response.contentType = Option(contentType)
  }

  /**
   * Sets the status code of the current response.
   */
  def status_=(code: Int): Unit = { response.status = code }

  /**
   * Explicitly sets the request-scoped format.  This takes precedence over
   * whatever was inferred from the request.
   */
  def format_=(formatValue: Symbol): Unit = {
    request(ApiFormats.FormatKey) = formatValue.name
  }

  /**
   * Explicitly sets the request-scoped format.  This takes precedence over
   * whatever was inferred from the request.
   */
  def format_=(formatValue: String): Unit = {
    request(ApiFormats.FormatKey) = formatValue
  }

  protected[this] implicit def scalatraContext: ScalatraContext = {
    new StableValuesContext()(HttpServletRequestReadOnly(request), response, servletContext)
  }

}
