package org.scalatra

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.net.URI

/**
 * Redirects unsecured requests to the corresponding secure URL.
 */
trait SslRequirement extends Handler { self: ScalatraKernel =>
  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    _request.withValue(req) {
      _response.withValue(res) {
        if(!req.isSecure) {
          val oldUri = new URI(req.getRequestURL.toString)
          val url = new URI("https", oldUri.getAuthority, oldUri.getPath, oldUri.getQuery, oldUri.getFragment).toString
          redirect(url)
        } else {
          super.handle(req, res)
        }
      }
    }
  }
}
