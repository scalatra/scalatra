package org.scalatra

import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import java.net.URI

/**
 * Redirects unsecured requests to the corresponding secure URL.
 */
trait SslRequirement extends Handler { 
  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    if(!req.isSecure) {
      val oldUri = new URI(req.getRequestURL.toString)
      val url = new URI("https", oldUri.getAuthority, oldUri.getPath, oldUri.getQuery, oldUri.getFragment).toString
      res.sendRedirect(url)
    } else {
      super.handle(req, res)
    }
  }
}
