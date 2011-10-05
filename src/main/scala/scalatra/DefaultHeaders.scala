package scalatra

import org.scalatra.scalate.ScalateSupport
import org.scalatra.ScalatraServlet
import java.util.Date

trait DefaultHeaders { this: ScalatraServlet =>
  after() {
    response.addHeader("Date", new Date().toString())
    response.addHeader("Content-Language", "en-US")
  }
}
