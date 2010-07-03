package org.scalatra

import javax.servlet.http._

class RichSession(session: HttpSession) {
  def apply(key:String) = session.getAttribute(key) match {
    case null => None
    case v: Any => Some(v)
  }
  
  def update(k:String, v:Object) = v match {
    case null => ()
    case _ => session.setAttribute(k,v)
  }
}
