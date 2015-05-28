package org.scalatra.servlet

import javax.servlet.http.{ HttpServletRequestWrapper, HttpServletRequest }

/**
 * Read-only immutable wrapper for an [[HttpServletRequest]] that can, for the most part, be
 * passed around to different threads.
 *
 * This is necessary because ServletContainers will "recycle" a request once the original HTTP
 * thread is returned, meaning that a lot of attributes are set to null (in the case of Jetty).
 *
 * Limitations of this class include the following:
 *
 *   - it is mostly immutable (methods on the original request are not given stable values,
 *     nor are methods that return non-primitive types)
 *   - changes made to the original object or this object may not be reflected across threads
 *
 * @param originalRequest the original HttpServletRequest to wrap
 */
case class HttpServletRequestReadOnly(private val originalRequest: HttpServletRequest) extends HttpServletRequestWrapper(originalRequest) {

  override val getAuthType: String = originalRequest.getAuthType

  override val getMethod: String = originalRequest.getMethod

  override val getPathInfo: String = originalRequest.getPathInfo

  override val getPathTranslated: String = originalRequest.getPathTranslated

  override val getContextPath: String = originalRequest.getContextPath

  override val getQueryString: String = originalRequest.getQueryString

  override val getRemoteUser: String = originalRequest.getRemoteUser

  override val getRequestedSessionId: String = originalRequest.getRequestedSessionId

  override val getRequestURI: String = originalRequest.getRequestURI

  override val getServletPath: String = originalRequest.getServletPath

  override val isRequestedSessionIdValid: Boolean = originalRequest.isRequestedSessionIdValid

  override val isRequestedSessionIdFromCookie: Boolean = originalRequest.isRequestedSessionIdFromCookie

  override val isRequestedSessionIdFromURL: Boolean = originalRequest.isRequestedSessionIdFromURL

  override val isRequestedSessionIdFromUrl: Boolean = originalRequest.isRequestedSessionIdFromUrl

  override val getCharacterEncoding: String = originalRequest.getCharacterEncoding

  override val getContentLength: Int = originalRequest.getContentLength

  override val getContentType: String = originalRequest.getContentType

  override val getContentLengthLong: Long = originalRequest.getContentLengthLong

  override val getProtocol: String = originalRequest.getProtocol

  override val getServerName: String = originalRequest.getServerName

  override val getScheme: String = originalRequest.getScheme

  override val getServerPort: Int = originalRequest.getServerPort

  override val getRemoteAddr: String = originalRequest.getRemoteAddr

  override val getRemoteHost: String = originalRequest.getRemoteHost

  override val isSecure: Boolean = originalRequest.isSecure

  override val getRemotePort: Int = originalRequest.getRemotePort

  override val getLocalName: String = originalRequest.getLocalName

  override val getLocalAddr: String = originalRequest.getLocalAddr

  override val getLocalPort: Int = originalRequest.getLocalPort

  override val isAsyncStarted: Boolean = originalRequest.isAsyncStarted

  override val isAsyncSupported: Boolean = originalRequest.isAsyncSupported

}
