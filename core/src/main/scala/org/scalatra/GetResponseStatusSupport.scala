package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponseWrapper, HttpServletResponse}
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mixin to ScalatraKernel that allows the retrieval of the HttpStatus.
 * Do not use with other response wrappers, or it will fail with a
 * ClassCastException.  This trait is not necessary if you can upgrade to
 * Servlet 3.0, which adds response.getStatus().
 */
trait GetResponseStatusSupport extends Handler { self: ScalatraKernel =>

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    super.handle(req, new ScalatraGetStatusServletResponseWrapper(res))
  }

  private class ScalatraGetStatusServletResponseWrapper(resp: HttpServletResponse) extends HttpServletResponseWrapper(resp) {

    private var _status = new AtomicInteger(200)
    def getStatus = {
      _status.get
    }

    override def setStatus(sc: Int) {
      _status.set(sc)
      resp setStatus sc
    }

    override def sendRedirect(location: String) {
      _status.set(HttpServletResponse.SC_MOVED_TEMPORARILY)
      resp.sendRedirect(location)
    }

    override def sendError(sc: Int) {
      _status.set(sc)
      resp.sendError(sc)
    }

    override def sendError(sc: Int, msg: String) {
      _status.set(sc)
      resp.sendError(sc, msg)
    }
  }

  /**
   * Returns the status of the current response.  Caution: if the response
   * has been wrapped by another class, this will throw a ClassCastException.
   */
  def status = response.asInstanceOf[ScalatraGetStatusServletResponseWrapper].getStatus
  def status_=(code: Int) = response.setStatus(code)
}
