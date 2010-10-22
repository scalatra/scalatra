package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.util.DynamicVariable

object FlashMapSupport {
  val sessionKey = FlashMapSupport.getClass.getName+".key"
}

trait FlashMapSupport extends Handler {
  import FlashMapSupport.sessionKey

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    _flash.withValue(getFlash(req)) {
      super.handle(req, res)
      flash.sweep()
      req.getSession.setAttribute(sessionKey, flash)
    }
  }

  private def getFlash(req: HttpServletRequest) =
    req.getSession.getAttribute(sessionKey) match {
      case flashMap: FlashMap => flashMap
      case _ => FlashMap()
    }


  private val _flash = new DynamicVariable[FlashMap](null)
  protected def flash = _flash.value
}