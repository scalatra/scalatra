package org.scalatra

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

case class ActionContext(request: HttpServletRequest, response: HttpServletResponse)
