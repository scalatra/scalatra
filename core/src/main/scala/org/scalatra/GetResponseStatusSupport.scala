package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponseWrapper, HttpServletResponse}

trait GetResponseStatusSupport extends Handler { self: ScalatraKernel =>

  def handle(req: HttpServletRequest, res: HttpServletResponse) {
    super.handle(req, new ScalatraGetStatusServletResponseWrapper(res))
  }

  private class ScalatraGetStatusServletResponseWrapper(resp: HttpServletResponse) extends HttpServletResponseWrapper(resp) {
    private var _status = 200
    def getStatus = _status
    override def setStatus(sc: Int) {
      _status = sc
      resp setStatus sc
    }
  }

  def status = response.asInstanceOf[ScalatraGetStatusServletResponseWrapper].getStatus
}