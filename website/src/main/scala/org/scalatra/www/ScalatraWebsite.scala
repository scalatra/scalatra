package org.scalatra.www

import java.net.URL
import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport

class ScalatraWebsite extends ScalatraServlet with ScalateSupport {

  get("/book/?"){
    redirect("/2.0.0/book/")
  }

  notFound {
    val templateBase = requestPath match {
      case s if s.endsWith("/") => s + "index"
      case s => s
    }
    val templatePath = templateBase + ".jade"
    servletContext.getResource(templatePath) match {
      case url: URL =>
        contentType = "text/html"
        templateEngine.layout(templatePath)
      case _ =>
        response.sendError(404)
    }
  }
}
