package org.scalatra

import test.scalatest.ScalatraFunSuite
import org.scalatest.matchers.MustMatchers

class CookieSupportServlet extends ScalatraServlet with CookieSupport {

  get("/getcookie") {
    cookies.get("somecookie") match {
      case Some(v) => v
      case _ => "None"
    }
  }

  post("/setcookie") {
    cookies.update("somecookie", params("cookieval"))
    "OK"
  }

  post("/setexpiringcookie") {
    cookies.update("thecookie", params("cookieval"))(CookieOptions(maxAge = params("maxAge").toInt))

  }
}

object CookieSupportTest extends ScalatraFunSuite with MustMatchers {
  val oneWeek = 7 * 24 * 3600

  addServlet(classOf[CookieSupportServlet], "/*")

  test("GET /getcookie with no cookies set should return 'None'") {
    get("/getcookie") {
      body must equal("None")
    }
  }

  test("POST /setcookie with a value should return OK") {
    post("/setcookie", "cookieval" -> "The value") {
      response.getHeader("Set-Cookie") must equal("somecookie=The+value")
    }
  }

  test("GET /getcookie with a cookie should set return the cookie value") {
    session {
      post("/setcookie", "cookieval" -> "The value") {
        body must equal("OK")
      }
      get("/getcookie") {
        body must equal("The value")
      }
    }
  }

  test("POST /setexpiringcookie should set the max age of the cookie") {
    post("/setexpiringcookie", "cookieval" -> "The value", "maxAge" -> oneWeek.toString) {
      response.getHeader("Set-Cookie") must equal("thecookie=The+value; Max-Age=604800")
    }
  }
}
