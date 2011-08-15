package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponseWrapper, HttpServletResponse}
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mixin to ScalatraKernel that allows the retrieval of the HttpStatus.
 * The response.getStatus() method was not added until Servlet 3.0.
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

  // TODO will fail if there is another response wrapper of a different type
  def status = response.asInstanceOf[ScalatraGetStatusServletResponseWrapper].getStatus
}
