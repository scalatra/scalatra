package scalatra

import org.scalatra.ScalatraServlet
import org.scalatra.scalate.ScalateSupport
import org.scalatra.ScalatraServlet
import org.scalatra.CookieSupport
import javax.servlet.http.HttpServletRequest
import org.fusesource.scalate.Binding
import scalatra.i18n.I18nScalateSupport

trait Controller extends 
   ScalatraServlet 
   with I18nScalateSupport
   with CookieSupport
   with DefaultHeaders {
  
  before() {
    contentType = "text/html"
  }
}
