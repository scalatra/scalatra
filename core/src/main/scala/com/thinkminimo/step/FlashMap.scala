package com.thinkminimo.step

import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}
import util.DynamicVariable
import collection.mutable.{Map => MMap}

object FlashMap {
  def apply(): FlashMap = new FlashMap
}

class FlashMap extends MMap[String, Any] {
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
  
  def sweep() {
    _now = next
    next = MMap()
    this
  }

  def keep() = {
    next ++= _now
    this
  }

  def keep(key: String) = {
    _now.get(key) foreach { value => next += ((key, value)) }
    this
  }

  def now = _now
}