package org.scalatra

import org.specs._
import org.specs.mock.Mockito

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

object CookieSupportSpec extends ScalatraSpec {

  val oneWeek = 7 * 24 * 3600
  addServlet(classOf[CookieSupportServlet], "/*")

  "GET /getcookie with no cookies set should return 'None'" in {
    get("/getcookie") {
      body must be_==("None")
    }
  }

  "POST /setcookie with a value should return OK" in {
    post("/setcookie", "cookieval" -> "The value") {
      response.getHeader("Set-Cookie") must beMatching("somecookie=\"The value\"")
    }
  }

  "GET /getcookie with a cookie should set return the cookie value" in {
    session {
      post("/setcookie", "cookieval" -> "The value") {
        body must be_==("OK")
      }
      get("/getcookie") {
        body must be_==("The value")
      }
    }
  }

  "POST /setexpiringcookie should set the max age of the cookie" in {
    post("/setexpiringcookie", "cookieval" -> "The value", "maxAge" -> oneWeek.toString) {
      response.getHeader("Set-Cookie") must beMatching("thecookie=\"The value\";Expires")
    }
  }
}
