package org.scalatra

import java.net.{MalformedURLException, URL}
import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest

class RichServletContext(sc: ServletContext) extends AttributesMap {
  /*
   * TODO The structural type works at runtime, but fails to compile because
   * of the raw type returned by getAttributeNames.  We're telling the
   * compiler to trust us; remove when we upgrade to Servlet 3.0.
   */
  protected def attributes = sc.asInstanceOf[Attributes]

  /**
   * Gets the resource mapped to the given path as an Option.
   */
  def resource(path: String): Option[URL] =
    try {
      Option(sc.getResource(path))
    }
    catch {
      case e: MalformedURLException => println("SHIT!: " + path); throw e
    }

  /**
   * Gets the resource mapped to the specified request path.  The
   * request path is defined as the servlet path plus the path info, if
   * any.
   */
  def resource(req: HttpServletRequest): Option[URL] = {
    val path = req.getServletPath + (Option(req.getPathInfo) getOrElse "")
    resource(path)
  }
}

