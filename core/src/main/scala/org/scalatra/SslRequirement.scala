package org.scalatra

import java.net.URI

/**
 * Redirects unsecured requests to the corresponding secure URL.
 */
trait SslRequirement extends Service { 
  this: Backend =>

  abstract override def apply(implicit req: Request, res: Response) = {
    if (!req.isSecure) {
      val oldUri = req.uri
      val port = securePortMap.lift(oldUri.getPort) getOrElse 443
      val uri = new URI(
	"https", oldUri.getRawUserInfo, oldUri.getHost, port,
	oldUri.getPath, oldUri.getQuery, oldUri.getFragment).toString
      res.redirect(uri)
      Some(())
    } else {
      super.apply(req, res)
    }
  }
  
  /**
   * Maps unsecured ports to secure ports.  By default, 80 redirects to
   * 443, and 8080 to 8443.
   */
  protected def securePortMap: PartialFunction[Int, Int] = 
    Map(80 -> 443, 8080 -> 8443)
}
