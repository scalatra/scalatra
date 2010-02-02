package com.thinkminimo.step

import javax.servlet.http._

object Session {
  implicit def httpServletRequestToSession(s:HttpServletRequest) = new Session(s)
}

class Session(request:HttpServletRequest) {
  def session = request.getSession(false) match {
    case null => None
    case (s:HttpSession) => {
      s.setMaxInactiveInterval(600)
      Some(s)
    }
  }
  def apply(key:String) = session match {
    case Some(s:HttpSession) =>
      s.getAttribute(key) match {
	case null => None
	case (v:Object) => Some(v)
      }
    case None => None
  }
  def update(k:String, v:Object) = v match {
    case null => ()
    case _ => request.getSession(true).setAttribute(k,v)
  }
  def invalidate = session map { _.invalidate }
}
