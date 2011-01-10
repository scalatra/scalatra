package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}
import collection.mutable.{Map => MMap}
import util.MutableMapWithIndifferentAccess

object FlashMap {
  def apply(): FlashMap = new FlashMap
}
/**
 * A FlashMap is the data structured used by [[org.scalatra.FlashMapSupport]] to allow passing temporary values between sequential actions.
 *
 * FlashMap behaves like [[org.scalatra.util.MapWithIndifferentAccess]] but anything you place in the flash in an action will be exposed only to the very next action and then cleared out.
 * @see FlashMapSupport
 */


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
