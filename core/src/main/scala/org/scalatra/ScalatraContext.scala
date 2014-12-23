package org.scalatra

import javax.servlet.ServletContext
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.servlet.ServletApiImplicits

object ScalatraContext {

  private class StableValuesContext(
    implicit val request: HttpServletRequest,
    val response: HttpServletResponse,
    val servletContext: ServletContext) extends ScalatraContext
}

trait ScalatraContext extends ServletApiImplicits with SessionSupport with CookieContext {

  import org.scalatra.ScalatraContext.StableValuesContext

  implicit def request: HttpServletRequest

  implicit def response: HttpServletResponse

  def servletContext: ServletContext

  /**
   * Gets the content type of the current response.
   */
  def contentType: String = response.contentType getOrElse null

  /**
   * Gets the status code of the current response.
   */
  def status: Int = response.status.code

  /**
   * Sets the content type of the current response.
   */
  def contentType_=(contentType: String): Unit = {
    response.contentType = Option(contentType)
  }

  @deprecated("Use status_=(Int) instead", "2.1.0")
  def status(code: Int): Unit = { status_=(code) }

  /**
   * Sets the status code of the current response.
   */
  def status_=(code: Int): Unit = { response.status = ResponseStatus(code) }

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
    new StableValuesContext()(request, response, servletContext)
  }

}