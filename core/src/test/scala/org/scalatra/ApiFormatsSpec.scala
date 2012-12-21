package org.scalatra

import test.specs2.MutableScalatraSpec
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

class ApiFormatsServlet extends ScalatraServlet with ApiFormatsAppBase
class ApiFormatsApp(config: ServletConfig, req: HttpServletRequest, res: HttpServletResponse) extends ScalatraApp(config, req, res) with ApiFormatsAppBase
trait ApiFormatsAppBase extends ApiFormats {
  override protected implicit def string2RouteMatcher(path: String): RouteMatcher = RailsPathPatternParser(path)

  get("/hello(.:format)") {
    format
  }

  get("/hi") {
    format
  }


}

class ApiFormatsServletSpec extends ApiFormatsSpecBase {
  mount(new ApiFormatsServlet, "/*")
}
class ApiFormatsAppSpec extends ApiFormatsSpecBase {
  mount(new ApiFormatsApp(_, _, _), "/*")
}
abstract class ApiFormatsSpecBase extends MutableScalatraSpec {



  "The ApiFormats" should {
    "get the format from the params" in {
      get("/hello.json") {
        val b = body
        response.getContentType must startWith("application/json")
        b must_== "json"
      }
    }

    "get the default format when no param has been given" in {
      get("/hello") {
        response.getContentType must startWith("text/html")
        body must_== "html"
      }
    }

    "get the format from a query string param" in {
      get("/hi?format=json") {
        response.getContentType must startWith("application/json")
        body must_== "json"
      }
    }

    "get the format from the accept header" in {
      "when there is only one format" in {
        get("/hello", headers = Map("Accept" -> "application/xml")) {
          response.getContentType must startWith("application/xml")
          body must_== "xml"
        }
      }

      "when the format is */*" in {
        get("/hello.xml", headers = Map("Accept" -> "*/*")) {
          response.getContentType must startWith("application/xml")
          body must_== "xml"
        }
      }

      "when there are multiple formats take the first match" in {
        get("/hello", headers = Map("Accept" -> "application/json, application/xml, text/plain, */*")) {
          response.getContentType must startWith("application/json")
          body must_== "json"
        }
      }

      "when there are multiple formats with priority take the first one with the highest weight" in {
         get("/hello", headers = Map("Accept" -> "application/json; q=0.4, application/xml; q=0.8, text/plain, */*")) {
           response.getContentType must startWith("text/plain")
           body must_== "txt"
         }
       }


      "when there is a content type which contains the default format, it should match" in {
        get("/hello", headers = Map("Content-Type" -> "application/xml,application/xhtml+xml,text/html")) {
          response.getContentType must startWith("text/html")
          body must_== "html"
        }
      }
    }

  }
}
