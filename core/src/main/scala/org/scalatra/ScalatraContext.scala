package org.scalatra

import javax.servlet.ServletContext
import javax.servlet.http.{ HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse, HttpServletResponseWrapper }

import org.scalatra.servlet.ServletApiImplicits

object ScalatraContext {

  private class StableValuesContext(
    implicit val request: HttpServletRequest,
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
    val reqWrap = new HttpServletRequestWrapper(request) {
      // Stable copies of things.
      override val getAuthType: String = request.getAuthType

      override val getMethod: String = request.getMethod

      override val getPathInfo: String = request.getPathInfo

      override val getPathTranslated: String = request.getPathTranslated

      override val getContextPath: String = request.getContextPath

      override val getQueryString: String = request.getQueryString

      override val getRemoteUser: String = request.getRemoteUser

      override val getRequestedSessionId: String = request.getRequestedSessionId

      override val getRequestURI: String = request.getRequestURI

      override val getServletPath: String = request.getServletPath

      override val isRequestedSessionIdValid: Boolean = request.isRequestedSessionIdValid

      override val isRequestedSessionIdFromCookie: Boolean = request.isRequestedSessionIdFromCookie

      override val isRequestedSessionIdFromURL: Boolean = request.isRequestedSessionIdFromURL

      override val isRequestedSessionIdFromUrl: Boolean = request.isRequestedSessionIdFromUrl

      override val getCharacterEncoding: String = request.getCharacterEncoding

      override val getContentLength: Int = request.getContentLength

      override val getContentType: String = request.getContentType

      override val getContentLengthLong: Long = request.getContentLengthLong

      override val getProtocol: String = request.getProtocol

      override val getServerName: String = request.getServerName

      override val getScheme: String = request.getScheme

      override val getServerPort: Int = request.getServerPort

      override val getRemoteAddr: String = request.getRemoteAddr

      override val getRemoteHost: String = request.getRemoteHost

      override val isSecure: Boolean = request.isSecure

      override val getRemotePort: Int = request.getRemotePort

      override val getLocalName: String = request.getLocalName

      override val getLocalAddr: String = request.getLocalAddr

      override val getLocalPort: Int = request.getLocalPort

      override val isAsyncStarted: Boolean = request.isAsyncStarted

      override val isAsyncSupported: Boolean = request.isAsyncSupported

    }
    val respWrap = new HttpServletResponseWrapper(response)
    new StableValuesContext()(reqWrap, respWrap, servletContext)
  }

}