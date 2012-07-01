package org.scalatra

import javax.servlet.http.{Cookie => HttpServletCookie}

package object servlet {

  implicit def servletCookieToRequestCookie(orig: HttpServletCookie) =
    RequestCookie(orig.getName, orig.getValue, CookieOptions(orig.getDomain, orig.getPath, orig.getMaxAge, comment = orig.getComment))

}