package org.scalatra

import scala.collection.JavaConversions._
import javax.servlet.http.HttpSession
import java.util.Enumeration
import collection.mutable.{MapLike, Map}


class RichSession(session: HttpSession) extends Map[String, AnyRef] {
  def get(key:String) = session.getAttribute(key) match {
    case null => None
    case v: AnyRef => Some(v)
  }

  def iterator = session.getAttributeNames.asInstanceOf[Enumeration[String]] map { key =>
    (key, session.getAttribute(key))
  }

  def +=(kv: (String, AnyRef)) = {
    session.setAttribute(kv._1, kv._2)
    this
  }

  def -=(key: String) = {
    session.removeAttribute(key)
    this
  }
}
