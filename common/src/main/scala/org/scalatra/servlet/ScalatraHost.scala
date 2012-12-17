package org.scalatra.servlet

import org.scalatra.Handler
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}

final class ScalatraHost[T <: Handler](factory: (ServletConfig, HttpServletRequest, HttpServletResponse) => T) extends HttpServlet  {

  private[this] var conf: ServletConfig = _
  override def init(config: ServletConfig) {
    conf = config
    super.init(config)
  }

  override def service(req: HttpServletRequest, res: HttpServletResponse) {
    val app = factory(conf, req, res)
    app.handle(req, res)
  }
}