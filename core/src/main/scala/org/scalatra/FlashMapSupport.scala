package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.util.DynamicVariable

object FlashMapSupport {
  val sessionKey = FlashMapSupport.getClass.getName+".key"
}

/**
 * Allows an action to set key-value pairs in a transient state that is accessible only to the next action and is expired immediately after that.
 * This is especially useful when using the POST-REDIRECT-GET pattern to trace the result of an operation.
 * {{{
 * post("/article/create") {
 *   // create session
 *   flash("notice") = "article created succesfully"
 *   redirect("/home")
 * }
 * get("/home") {
 *   // this will access the value set in previous action
 *   stuff_with(flash("notice"))
 * }
 * }}}
 * @see FlashMap
 */
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

  /**
   * returns a thread local [[org.scalatra.FlashMap]] instance
   */
  protected def flash = _flash.value
}
