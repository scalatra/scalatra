package org

import javax.servlet.http.HttpSession
import org.scalatra.util.MultiMap

package object scalatra {
  /**
   * Structural type for the various Servlet API objects that have attributes.  These include ServletContext,
   * HttpSession, and ServletRequest.
   */
  type Attributes = {
    def getAttribute(name: String): AnyRef
    def getAttributeNames(): java.util.Enumeration[_]
    def setAttribute(name: String, value: AnyRef): Unit
    def removeAttribute(name: String): Unit
  }

  @deprecated("Use CsrfTokenSupport")
  type CSRFTokenSupport = CsrfTokenSupport

  type RouteMatcher = () => Option[ScalatraKernel.MultiParams]

  type ErrorHandler = PartialFunction[Throwable, Any]
}
