package org.scalatra

import org.specs._
import org.specs.mock.Mockito
import test.ScalatraTests
import test.specs.ScalatraSpecification

class CookieSupportServlet extends ScalatraServlet with CookieSupport {

  get("/getcookie") {
    cookies("somecookie") match {
      case Some(v:String) => v
      case _ => "None"
    }
  }

  post("/setcookie") {
    cookies.update("somecookie", params("cookieval"))
    "OK"
  }

  post("/setexpiringcookie") {
    cookies.update("thecookie", params("cookieval"), CookieOptions(maxAge = params("maxAge").toInt))

  }
}

// TODO Understand Specs idiom to configure the ServletTester.  doBeforeSpec?  doAroundExpectations?  Nothing worked.
object CookieSupportSpec extends Specification with ScalatraTests {
  val oneWeek = 7 * 24 * 3600

  def withServletTester(f: =>Any) = {
    addServlet(classOf[CookieSupportServlet], "/*")
    start()
    f
    println("STOP")
    stop()
  }

  "GET /getcookie with no cookies set should return 'None'" in {
    withServletTester {
      get("/getcookie") {
        body must be_==("None")
      }
      stop()
    }
  }

  "POST /setcookie with a value should return OK" in {
    withServletTester {
      post("/setcookie", "cookieval" -> "The value") {
        response.getHeader("Set-Cookie") must beMatching("somecookie=\"The value\"")
      }
    }
  }

  "GET /getcookie with a cookie should set return the cookie value" in {
    withServletTester {
      session {
        post("/setcookie", "cookieval" -> "The value") {
          body must be_==("OK")
        }
        get("/getcookie") {
          body must be_==("The value")
        }
      }
    }
  }

  "POST /setexpiringcookie should set the max age of the cookie" in {
    withServletTester {
      post("/setexpiringcookie", "cookieval" -> "The value", "maxAge" -> oneWeek.toString) {
        response.getHeader("Set-Cookie") must beMatching("thecookie=\"The value\";Expires")
      }
    }
  }
}
