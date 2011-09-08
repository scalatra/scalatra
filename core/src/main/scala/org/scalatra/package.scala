package org

import javax.servlet.http.HttpSession
import org.scalatra.util.MultiMap

package object scalatra {
  /**
   * Structural type for the various Servlet API objects that have attributes.
   * These include ServletContext, HttpSession, and ServletRequest.
   */
  type Attributes = {
    def getAttribute(name: String): AnyRef
    def getAttributeNames(): java.util.Enumeration[_]
    def setAttribute(name: String, value: AnyRef): Unit
    def removeAttribute(name: String): Unit
  }

  @deprecated("Use CsrfTokenSupport", "2.0")
  type CSRFTokenSupport = CsrfTokenSupport

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]
}
