package org.scalatra
package servlet

import jakarta.servlet.http.HttpSession

/**
 * Extension methods to the standard HttpSession.
 */
case class RichSession(session: HttpSession) extends AttributesMap {

  def id: String = session.getId

  protected[this] type A = HttpSession
  protected[this] override def attributes = session
  protected[this] override def attributesTypeClass: Attributes[A] = Attributes[A]

}
