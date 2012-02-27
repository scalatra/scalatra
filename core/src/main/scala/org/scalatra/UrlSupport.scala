package org.scalatra

import java.net.URLEncoder.encode
import javax.servlet.http.HttpServletResponse

/**
 * Provides utility methods for the creation of URL strings.
 * Supports context-relative and session-aware URLs.  Should behave similarly to JSTL's <c:url> tag.
 */
@deprecated("This functionality has been subsumed by ScalatraService.") // Since 2.1
trait UrlSupport
