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
      val port = securePortMap.lift(oldUri.getPort) getOrElse 443
      val url = new URI(
	"https", oldUri.getRawUserInfo, oldUri.getHost, port,
	oldUri.getPath, oldUri.getQuery, oldUri.getFragment).toString
      res.sendRedirect(url)
    } else {
      super.handle(req, res)
    }
  }
  
  /**
   * Maps unsecured ports to secure ports.  By default, 80 redirects to
   * 443, and 8080 to 8443.
   */
  protected def securePortMap: PartialFunction[Int, Int] = 
    Map(80 -> 443, 8080 -> 8443)
}
