package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponseWrapper, HttpServletResponse}
import java.util.concurrent.atomic.AtomicInteger

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
  }

  def status = response.asInstanceOf[ScalatraGetStatusServletResponseWrapper].getStatus
}