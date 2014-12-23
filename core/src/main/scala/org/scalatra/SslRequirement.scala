package org.scalatra

import java.net.URI
import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.scalatra.servlet.ServletApiImplicits

/**
 * Redirects unsecured requests to the corresponding secure URL.
 */
trait SslRequirement extends Handler with ServletApiImplicits {

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    if (!req.isSecure) {
      val oldUri = req.uri
      val port = securePortMap.lift(oldUri.getPort) getOrElse 443
      val uri = new URI(
        "https",
        oldUri.getRawUserInfo,
        oldUri.getHost,
        port,
        oldUri.getPath,
        oldUri.getQuery,
        oldUri.getFragment
      ).toString
      res.redirect(uri)
    } else {
      super.handle(req, res)
    }
  }

  /**
   * Maps unsecured ports to secure ports.
   * By default, 80 redirects to 443, and 8080 to 8443.
   */
  protected def securePortMap: PartialFunction[Int, Int] = {
    Map(80 -> 443, 8080 -> 8443)
  }

}
