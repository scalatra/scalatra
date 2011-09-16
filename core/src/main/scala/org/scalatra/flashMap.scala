package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}
import scala.collection.mutable.{Map => MMap, Set => MSet}
import scala.util.DynamicVariable
import util.MutableMapWithIndifferentAccess

/**
 * A FlashMap is the data structure used by [[org.scalatra.FlashMapSupport]]
 * to allow passing temporary values between sequential actions.
 *
 * FlashMap behaves like [[org.scalatra.util.MapWithIndifferentAccess]].  By
 * default, anything placed in the map is available to the current request and
 * next request, and is then discarded.
 *
 * @see FlashMapSupport
 */
@serializable
class FlashMap extends MutableMapWithIndifferentAccess[Any] {
  private val m = MMap[String, Any]()
  private val flagged = MSet[String]()

  /**
   * Removes an entry from the flash map.  It is no longer available for this
   * request or the next.
   */
  def -=(key: String) = {
    m -= key
    this
  }

  /**
   * Adds an entry to the flash map.  Clears the sweep flag for the key.
   */
  def +=(kv: (String, Any)) = {
    flagged -= kv._1
    m += kv
    this
  }

  /**
   * Creates a new iterator over the values of the flash map.  These are the
   * values that were added during the last request.
   */
  def iterator = new Iterator[(String, Any)] {
    private val it = m.iterator

    def hasNext = it.hasNext

    def next = {
      val kv = it.next
      flagged += kv._1
      kv
    }
  }

  /**
   * Returns the value associated with a key and flags it to be swept.
   */
  def get(key: String) = {
    flagged += key
    m.get(key)
  }

  /**
   * Removes all flagged entries.
   */
  def sweep() {
    flagged foreach { key => m -= key }
  }

  /**
   * Clears all flags so no entries are removed on the next sweep.
   */
  def keep() {
    flagged.clear()
  }

  /**
   * Clears the flag for the specified key so its entry is not removed on the next sweep.
   */
  def keep(key: String) {
    flagged -= key
  }

  /**
   * Flags all current keys so the entire map is cleared on the next sweep.
   */
  def flag() {
    flagged ++= m.keys
  }

  /**
   * Sets a value for the current request only.  It will be removed before the next request unless explicitly kept.
   * Data put in this object is availble as usual:
   * {{{
   * flash.now("notice") = "logged in succesfully"
   * flash("notice") // "logged in succesfully"
   * }}}
   */
  object now {
    def update(key: String, value: Any) =  {
      flagged += key
      m += key -> value
    }
  }
}

object FlashMapSupport {
  val sessionKey = FlashMapSupport.getClass.getName+".flashMap"
  val lockKey = FlashMapSupport.getClass.getName+".lock"
  val FlashMapKey = "org.scalatra.FlashMap".intern
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
trait FlashMapSupport extends ScalatraKernel {
  import FlashMapSupport._

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
      val f = getFlash(req)
      val isOutermost = !req.contains(lockKey)
      if (isOutermost) {
        req(lockKey) = "locked"
        if (sweepUnusedFlashEntries(req)) {
          f.flag()
        }
      }
      req.getSession.setAttribute(sessionKey, f)
      super.handle(req, res)
      /*
       * http://github.org/scalatra/scalatra/issues/41
       * http://github.org/scalatra/scalatra/issues/57
       *
       * Only the outermost FlashMapSupport sweeps it at the end.  This deals with both nested filters and
       * redirects to other servlets.
       */
      if (isOutermost) {
        f.sweep()
      }
  }

  private def getFlash(req: HttpServletRequest) =
    req.getSession.getAttribute(sessionKey) match {
      case flashMap: FlashMap => flashMap
      case _ => new FlashMap()
    }

  /**
   * Returns the [[org.scalatra.FlashMap]] instance for the current request.
   */
  def flash = getFlash(request)

  /**
   * Determines whether unused flash entries should be swept.  The default is false.
   */
  protected def sweepUnusedFlashEntries(req: HttpServletRequest) = false
}
