package org.scalatra
package netty

import test.specs2.ScalatraSpec

class CookiesTestApp extends ScalatraApp {
  get("/getcookie") {
    request.cookies.get("anothercookie") foreach { cookie =>
      response.headers += "X-Another-Cookie" -> cookie
    }
    request.cookies.get("somecookie") match {
      case Some(v) => v
      case _ => "None"
    }
  }

  post("/setcookie") {
    request.cookies update ("somecookie", params("cookieval"))
    params.get("anothercookieval") foreach { request.cookies += "anothercookie" -> _ }
    "OK"
  }

  post("/setexpiringcookie") {
    request.cookies.update("thecookie", params("cookieval"))(CookieOptions(maxAge = params("maxAge").toInt))
  }

  post("/set-http-only-cookie") {
    request.cookies.update("thecookie", params("cookieval"))(CookieOptions(httpOnly = true))
  }

  post("/maplikeset") {
    request.cookies += "somecookie" -> params("cookieval")
    "OK"
  }

  post("/remove-cookie") {
    request.cookies -= "somecookie"
    response.headers += "Somecookie-Is-Defined" -> request.cookies.get("somecookie").isDefined.toString
    "OK"
  }

  post("/remove-cookie-with-path") {
    request.cookies.-=("somecookie")(CookieOptions(path = "/bar"))
    "OK"
  }

  error {
    case e => e.printStackTrace()
  }
}

class CookieSupportSpec extends ScalatraSpec with NettyBackend {

  mount("/foo", new CookiesTestApp)

  def is =
    "CookieSupport should" ^
      "GET /getcookie with no cookies set should return 'None'" ! noCookies ^
      "POST /setcookie with a value should return OK" ! setsCookie ^
      "GET /getcookie with a cookie should set return the cookie value" ! returnsSetCookie ^
      "POST /setexpiringcookie should set the max age of the cookie" ! setsExpiringCookie ^
      "cookie path defaults to app path" ! defaultsToAppPath ^
      "cookie path defaults to app path when using a maplike setter" ! defaultToAppPathMap ^
      "handles multiple cookies" ! handlesMultiple ^
      "removes a cookie by setting max-age = 0" ! removesCookie ^
      "removes a cookie by setting a path" ! removesCookieWithPath ^
      "respects the HttpOnly option" ! supportsHttpOnly ^
      "removing a cookie removes it from the map view" ! removingCookieRemovesFromMap ^
    end

  def noCookies = {
    get("/foo/getcookie") {
      response.body must_== "None"
    }
  }

  def setsCookie = {
    post("/foo/setcookie", "cookieval" -> "The value") {
      response.headers("Set-Cookie") must startWith("""somecookie=The value;""") and
      (response.cookies("somecookie").value must_== "The value")
    }
  }

  def returnsSetCookie = {
    session {
      val res1 = post("/foo/setcookie", "cookieval" -> "The value") {
        body must_== "OK"
      }
      val res2 = get("/foo/getcookie") {
        body must_== "The value"
      }
      res1 and res2
    }
  }

  def setsExpiringCookie = {
    post("/foo/setexpiringcookie", "cookieval" -> "The value", "maxAge" -> 604800.toString) {
      response.headers("Set-Cookie") must_== """thecookie=The value; Max-Age=604800"""
    }
  }

  def defaultsToAppPath = {
    post("/foo/setcookie", "cookieval" -> "whatever") {
      response.headers("Set-Cookie") must beMatching(".+;.*Path=/foo")
    }
  }

  def defaultToAppPathMap = {
    post("/foo/maplikeset", "cookieval" -> "whatever") {
      val cookie = response.cookies("somecookie")

      (cookie.value must_== "whatever") and (cookie.cookieOptions.path must_== "/foo")
    }
  }

    // This is as much a test of ScalatraTests as it is of CookieSupport.
    // http://github.com/scalatra/scalatra/issue/84
  def handlesMultiple = {
    session {
      val res1 = post("/foo/setcookie", "cookieval" -> "The value", "anothercookieval" -> "Another Cookie") {
        body must_== "OK"
      }
      val res2 = get("/foo/getcookie") {
        (body must_== "The value") and
        (headers.get("X-Another-Cookie") must_== Some("Another Cookie"))
      }
      res1 and res2
    }
  }

  def removesCookie = {
    post("/foo/remove-cookie") {
      val hdr = response.headers("Set-Cookie")
      // Jetty turns Max-Age into Expires
      hdr must contain ("; Max-Age=0")
    }
  }

  def removesCookieWithPath = {
    post("/foo/remove-cookie-with-path") {
      val hdr = response.headers("Set-Cookie")
      // Jetty turns Max-Age into Expires
      (hdr must contain("; Max-Age=0")) and
      (hdr must contain("; Path=/bar"))
    }
  }

  def supportsHttpOnly = {
    post("/foo/set-http-only-cookie", "cookieval" -> "whatever") {
      val hdr = response.headers("Set-Cookie")
      hdr must beMatching(".+;.*HttpOnly")
    }
  }

  def removingCookieRemovesFromMap = {
    session {
      post("/foo/setcookie", "cookieval" -> "whatever") {}
      post("/foo/remove-cookie") {
        headers("Somecookie-Is-Defined") must_== "false"
      }
    }
  }


  /*
    test("removing a cookie removes it from the map view") {
    }
   */
}
