package org.scalatra
package servlet

import javax.servlet.ServletContext
import javax.servlet.http.{HttpServletResponse, HttpSession}

trait ServletHandler extends Handler {
  type RequestT = ServletRequest
  type ResponseT = ServletResponse
}
