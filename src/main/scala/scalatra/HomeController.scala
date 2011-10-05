package scalatra

import org.scalatra.scalate.ScalateSupport
import org.scalatra.ScalatraServlet
import org.scalatra.CookieSupport
import org.fusesource.scalate.Binding

class HomeController extends Controller {
  // http://localhost:8080/?locale=en_AU
  // http://localhost:8080/?locale=en_US
  get("/") {
    println("hello=" + messages.get("hello").getOrElse("world").format(5))
    println("something=" + messages.get("something").getOrElse("number %d").format(5))
    jade("/hello")
  }

  notFound {
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound() 
  }
}
