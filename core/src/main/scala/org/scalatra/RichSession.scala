package org.scalatra

import javax.servlet.http.HttpSession
import util.MapWithIndifferentAccess

/**
 * Extension methods to the standard HttpSession.
 */
class RichSession(session: HttpSession) extends AttributesMap {
  /*
   * TODO The structural type works at runtime, but fails to compile because
   * of the raw type returned by getAttributeNames.  We're telling the
   * compiler to trust us; remove when we upgrade to Servlet 3.0.
   */
  protected def attributes = session.asInstanceOf[Attributes]
}
