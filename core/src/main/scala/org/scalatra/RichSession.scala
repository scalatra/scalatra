package org.scalatra

import javax.servlet.http._
import util.MapWithIndifferentAccess

class RichSession(session: HttpSession) extends MapWithIndifferentAccess[Option[AnyRef]] {
  def apply(key:String) = session.getAttribute(key) match {
    case null => None
    case v: Any => Some(v)
  }
  
  def update(k:String, v:Object) = v match {
    case null => ()
    case _ => session.setAttribute(k,v)
  }
}
