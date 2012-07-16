package org.scalatra.servlet

import org.scalatra._
import org.scalatra.store.session.InMemorySessionStore

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse} 

/**
* A Scalatra servlet backend which forwards requests to a ScalatraApp.
*/
class ScalatraServlet(appContext: AppContext) extends HttpServlet {

  override def service(request: HttpServletRequest, response: HttpServletResponse) {
    val req = new ServletHttpRequest(request)
    val res = new ServletHttpResponse(response, req.cookies)

    appContext.application(req) match {
      case Some(app: ScalatraApp with SessionSupport) =>
        val current = req.cookies.get(appContext.sessionIdKey).flatMap(appContext.sessions.get).getOrElse(appContext.sessions.newSession)
        if (current.isEmpty) req.cookies += appContext.sessionIdKey -> app.session.id
        app.handle(req, res)

      case Some(app: ScalatraApp) =>
        app.handle(req, res)

      case None =>
        response.setStatus(404)
    }
  }

}
