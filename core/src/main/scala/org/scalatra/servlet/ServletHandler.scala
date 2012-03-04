package org.scalatra
package servlet

import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletRequest, HttpServletResponse, HttpSession}

trait ServletHandler extends Handler with ServletApiImplicits {
  type RequestT = HttpServletRequest
  type ResponseT = HttpServletResponse
}
