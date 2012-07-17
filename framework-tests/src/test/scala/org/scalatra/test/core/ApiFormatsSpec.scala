package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.specs.ScalatraSpecification

class ApiFormatsApp extends ScalatraApp with ApiFormats {
  override protected implicit def string2RouteMatcher(path: String): RouteMatcher = RailsPathPatternParser(path)

   get("/hello(.:format)") {
    format
  }

  error {
    case ex => ex.printStackTrace()
  }


}

abstract class ApiFormatsSpec extends ScalatraSpecification {

  mount(new ApiFormatsApp)

  "The ApiFormats" should {
    "get the format from the params" in {
      get("/hello.json") {
        val b = body
        response.contentType must startWith("application/json")
        b must_== "json"
      }
    }

    "get the default format when no param has been given" in {
      get("/hello") {
        response.contentType must startWith("text/html")
        body must_== "html"
      }
    }

    "get the format from the accept header" in {
      "when there is only one format" in {
        get("/hello", headers = Map("Accept" -> "application/xml")) {
          response.contentType must startWith("application/xml")
          body must_== "xml"
        }
      }

      "when the format is */*" in {
        get("/hello.xml", headers = Map("Accept" -> "*/*")) {
          response.contentType must startWith("application/xml")
          body must_== "xml"
        }
      }

      "when there are multiple formats take the first match" in {
        get("/hello", headers = Map("Accept" -> "application/json, application/xml, text/plain, */*")) {
          response.contentType must startWith("application/json")
          body must_== "json"
        }
      }

      "when there are multiple formats with priority take the first one with the highest weight" in {
         get("/hello", headers = Map("Accept" -> "application/json; q=0.4, application/xml; q=0.8, text/plain, */*")) {
           response.contentType must startWith("text/plain")
           body must_== "txt"
         }
       }


      "when there is a content type which contains the default format, it should match" in {
        get("/hello", headers = Map("Content-Type" -> "text/html")) {
          response.contentType must startWith("text/html")
          body must_== "html"
        }
      }
    }

  }
}

class NettyApiFormatsSpec extends ApiFormatsSpec with NettyBackend
class JettyApiFormatsSpec extends ApiFormatsSpec with JettyBackend
