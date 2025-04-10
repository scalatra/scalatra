package org.scalatra

import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListSet}
import org.scalatra.ServletCompat.http.{HttpServletRequest, HttpServletResponse}

import scala.jdk.CollectionConverters.*

/** A FlashMap is the data structure used by [[org.scalatra.FlashMapSupport]] to allow passing temporary values between
  * sequential actions.
  *
  * As of Scalatra 2.7.x, it does not directly inherit Map.
  *
  * @see
  *   FlashMapSupport
  */
class FlashMap extends Serializable {

  private[this] val m = new ConcurrentHashMap[String, Any]().asScala

  private[this] val flagged = new ConcurrentSkipListSet[String]().asScala

  /** Adds an entry to the flash map. Clears the sweep flag for the key.
    */
  def update(key: String, value: Any): Unit = {
    flagged -= key
    m.update(key, value)
  }

  /** Removes an entry from the flash map. It is no longer available for this request or the next.
    */
  def remove(key: String): Any = {
    m.remove(key)
  }

  /** Creates a new iterator over the values of the flash map. These are the values that were added during the last
    * request.
    */
  def iterator = new Iterator[(String, Any)] {
    private[this] val it = m.iterator

    def hasNext = it.hasNext

    def next() = {
      val kv = it.next()
      flagged += kv._1
      kv
    }
  }

  /** Returns the value associated with a key and flags it to be swept.
    */
  def get(key: String): Option[Any] = {
    flagged += key
    m.get(key)
  }

  /** Returns the value associated with a key and flags it to be swept.
    */
  def apply(key: String): Any = {
    flagged += key
    m(key)
  }

  /** Removes all flagged entries.
    */
  def sweep(): Unit = {
    flagged foreach { key => m -= key }
  }

  /** Clears all flags so no entries are removed on the next sweep.
    */
  def keep(): Unit = {
    flagged.clear()
  }

  /** Clears the flag for the specified key so its entry is not removed on the next sweep.
    */
  def keep(key: String): Unit = {
    flagged -= key
  }

  /** Flags all current keys so the entire map is cleared on the next sweep.
    */
  def flag(): Unit = {
    flagged ++= m.keys
  }

  /** Convert to Set
    */
  def toSet: Set[(String, Any)] = m.toSet

  /** Sets a value for the current request only. It will be removed before the next request unless explicitly kept. Data
    * put in this object is available as usual:
    * {{{
    * flash.now("notice") = "logged in successfully"
    * flash("notice") // "logged in successfully"
    * }}}
    */
  object now {

    def update(key: String, value: Any) = {
      flagged += key
      m += key -> value
    }
  }
}

object FlashMapSupport {

  val SessionKey = FlashMapSupport.getClass.getName + ".flashMap"

  val LockKey = FlashMapSupport.getClass.getName + ".lock"

  val FlashMapKey = "org.scalatra.FlashMap"

}

/** Allows an action to set key-value pairs in a transient state that is accessible only to the next action and is
  * expired immediately after that. This is especially useful when using the POST-REDIRECT-GET pattern to trace the
  * result of an operation.
  * {{{
  * post("/article/create") {
  *   // create session
  *   flash("notice") = "article created successfully"
  *   redirect("/home")
  * }
  * get("/home") {
  *   // this will access the value set in previous action
  *   stuff_with(flash("notice"))
  * }
  * }}}
  * @see
  *   FlashMap
  */
trait FlashMapSupport extends Handler {
  this: ScalatraBase =>

  import org.scalatra.FlashMapSupport.*

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    withRequest(req) {
      val f           = flash
      val isOutermost = !request.contains(LockKey)

      ScalatraBase onCompleted { _ =>
        /*
         * https://github.com/scalatra/scalatra/issues/41
         * https://github.com/scalatra/scalatra/issues/57
         *
         * Only the outermost FlashMapSupport sweeps it at the end.
         * This deals with both nested filters and redirects to other servlets.
         */
        if (isOutermost) {
          f.sweep()
        }
        flashMapSetSession(f)
      }

      if (isOutermost) {
        req(LockKey) = "locked"
        if (sweepUnusedFlashEntries(req)) {
          f.flag()
        }
      }

      super.handle(req, res)
    }
  }

  /** Override to implement custom session retriever, or sanity checks if session is still active
    * @param f
    */
  def flashMapSetSession(f: FlashMap): Unit = {
    try {
      // Save flashMap to Session after (a session could stop existing during a request, so catch exception)
      session(SessionKey) = f
    } catch {
      case e: Throwable =>
    }
  }

  private[this] def getFlash(req: HttpServletRequest): FlashMap =
    req.get(SessionKey).map(_.asInstanceOf[FlashMap]).getOrElse {
      val map = session
        .get(SessionKey)
        .map {
          _.asInstanceOf[FlashMap]
        }
        .getOrElse(new FlashMap)

      req.setAttribute(SessionKey, map)
      map
    }

  /** Returns the [[org.scalatra.FlashMap]] instance for the current request.
    */
  def flash(implicit request: HttpServletRequest): FlashMap = getFlash(request)

  def flash(key: String)(implicit request: HttpServletRequest): Any = getFlash(request)(key)

  /** Determines whether unused flash entries should be swept. The default is false.
    */
  protected def sweepUnusedFlashEntries(req: HttpServletRequest): Boolean = false

}
