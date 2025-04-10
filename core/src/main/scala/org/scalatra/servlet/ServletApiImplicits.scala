package org.scalatra
package servlet

import org.scalatra.ServletCompat.ServletContext
import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpServletResponse, HttpSession}

trait ServletApiImplicits {

  implicit def enrichRequest(request: HttpServletRequest): RichRequest =
    RichRequest(request)

  implicit def enrichResponse(response: HttpServletResponse): RichResponse =
    RichResponse(response)

  implicit def enrichSession(session: HttpSession): RichSession =
    RichSession(session)

  implicit def enrichServletContext(servletContext: ServletContext): RichServletContext =
    RichServletContext(servletContext)

}

object ServletApiImplicits extends ServletApiImplicits
