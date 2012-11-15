package org.scalatra

import javax.servlet.http.{HttpServlet, HttpServletResponse, HttpServletRequest}
import javax.servlet.{ServletConfig, ServletResponse, ServletRequest}

trait AppReqResScope extends RequestResponseScope {

  protected def withRequestResponse[A](request: HttpServletRequest, response: HttpServletResponse)(f: => A): A = f

  /**
   * Executes the block with the given request bound to the `request`
   * method.
   */
  protected def withRequest[A](request: HttpServletRequest)(f: => A): A = f

  /**
   * Executes the block with the given response bound to the `response`
   * method.
   */
  protected def withResponse[A](response: HttpServletResponse)(f: => A): A = f
}

abstract class ScalatraApp(implicit val request: HttpServletRequest, implicit val response: HttpServletResponse) extends ScalatraSyntax with AppReqResScope with Initializable {

}

class ScalatraHost(factory: (HttpServletRequest, HttpServletResponse) => ScalatraApp) extends HttpServlet  {

  private[this] var conf: ServletConfig = _
  override def init(config: ServletConfig) {
    conf = config
    super.init(config)
  }

  override def service(req: HttpServletRequest, res: HttpServletResponse) {
    val app = factory(req, res)
    app.initialize(conf.asInstanceOf[app.ConfigT])
    app.handle(req, res)
  }
}