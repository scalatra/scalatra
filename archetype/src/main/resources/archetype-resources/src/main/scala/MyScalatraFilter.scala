package net.srirangan

import org.scalatra._
import java.net.URL
import scalate.ScalateSupport

class MyScalatraFilter extends ScalatraFilter with ScalateSupport {
  
  get("/") {
    <html>
      <body>
        <h1>Hello, world!</h1>
        Saying hello to Scalatra from Maven.
      </body>
    </html>
  }

  notFound {
    // If no route matches, then try to render a Scaml template
    val templateBase = requestPath match {
      case s if s.endsWith("/") => s + "index"
      case s => s
    }
    val templatePath = "/WEB-INF/scalate/templates/" + templateBase + ".scaml"
    servletContext.getResource(templatePath) match {
      case url: URL => 
        contentType = "text/html"
        templateEngine.layout(templatePath)
      case _ => 
        filterChain.doFilter(request, response)
    } 
  }
}
