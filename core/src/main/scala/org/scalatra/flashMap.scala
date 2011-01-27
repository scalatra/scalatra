package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}
import scala.collection.mutable.{Map => MMap}
import scala.util.DynamicVariable
import util.MutableMapWithIndifferentAccess

object FlashMap {
  def apply(): FlashMap = new FlashMap
}

/**
 * A FlashMap is the data structured used by [[org.scalatra.FlashMapSupport]]
 * to allow passing temporary values between sequential actions.
 *
 * FlashMap behaves like [[org.scalatra.util.MapWithIndifferentAccess]] but
 * anything you place in the flash in an action will be exposed only to the
 * very next action and then cleared out.
 * @see FlashMapSupport
 */
@serializable
class FlashMap extends MutableMapWithIndifferentAccess[Any] {
  private var _now = MMap[String, Any]()
  private var next = MMap[String, Any]()

  def -=(key: String) = {
    next -= key
    this
  }

  def +=(kv: (String, Any)) = {
    next += kv
    this
  }

  def iterator = _now.iterator

  def get(key: String) = _now.get(key)

  /**
   * removes all existing key-value pairs
   */
  def sweep() {
    _now = next
    next = MMap()
    this
  }

  /**
   * maintains present values available for the next action
   */
  def keep() = {
    next ++= _now
    this
  }

  /**
   * maintains the value associated with key `key` available for the next action
   */
  def keep(key: String) = {
    _now.get(key) foreach { value => next += ((key, value)) }
    this
  }

  /**
   * accesses the map that is availble in this action, not the next one.
   * Useful with filters and sub-methods. Data put in this object is availble as usual:
   * {{{
   * flash.now("notice") = "logged in succesfully"
   * flash("notice") // "logged in succesfully"
   * }}}
   */
  def now = _now
}

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
