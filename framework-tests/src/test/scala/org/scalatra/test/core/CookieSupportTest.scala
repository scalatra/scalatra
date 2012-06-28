package org.scalatra

import test.NettyBackend
import test.scalatest.ScalatraFunSuite
import java.net.{HttpCookie => JHttpCookie}

class CookieSupportApp extends ScalatraApp {

  get("/getcookie") {
    cookies.get("anothercookie") foreach { cookie =>
      response.headers += "X-Another-Cookie" -> cookie
    }
    cookies.get("somecookie") match {
      case Some(v) => v
      case _ => "None"
    }
  }

  post("/setcookie") {
    cookies.update("somecookie", params("cookieval"))
    params.get("anothercookieval") foreach { cookies("anothercookie") = _ }
    "OK"
  }

  post("/setexpiringcookie") {
    cookies.update("thecookie", params("cookieval"))(CookieOptions(maxAge = params("maxAge").toInt))
  }

  post("/set-http-only-cookie") {
    cookies.update("thecookie", params("cookieval"))(CookieOptions(httpOnly = true))
  }

  post("/maplikeset") {
    cookies += ("somecookie" -> params("cookieval"))
    "OK"
  }

  post("/remove-cookie") {
    cookies -= "somecookie"
    response.headers += "Somecookie-Is-Defined" -> cookies.get("somecookie").isDefined.toString
  }

  post("/remove-cookie-with-path") {
    cookies.delete("somecookie")(CookieOptions(path = "/bar"))
  }
}

abstract class CookieSupportTest extends ScalatraFunSuite {
  val oneWeek = 7 * 24 * 3600

  mount(new CookieSupportApp)
  mount("/foo", new CookieSupportApp)

  test("GET /getcookie with no cookies set should return 'None'") {
    get("/foo/getcookie") {
      body must equal("None")
    }
  }

  test("POST /setcookie with a value should return the value") {
    post("/foo/setcookie", "cookieval" -> "The value") {
      val cookie = JHttpCookie.parse(headers("Set-Cookie")).get(0)
      cookie.getValue must be ("The value")
    }
  }

  test("GET /getcookie with a cookie should set return the cookie value") {
    session {
      post("/foo/setcookie", "cookieval" -> "The value") {
        body must equal("OK")
      }
      get("/foo/getcookie") {
        body must equal("The value")
      }
    }
  }

  test("POST /setexpiringcookie should set the max age of the cookie") {
    post("/foo/setexpiringcookie", "cookieval" -> "The value", "maxAge" -> oneWeek.toString) {
      val cookie = JHttpCookie.parse(headers("Set-Cookie")).get(0)
      // Allow some slop, since it's a new call to currentTimeMillis
      cookie.getMaxAge.toInt must be (oneWeek plusOrMinus 10000)
    }
  }

  test("POST /set-http-only-cookie should set the HttpOnly flag of the cookie") {
    post("/foo/set-http-only-cookie", "cookieval" -> "whatever") {
      headers("Set-Cookie") must include.regex(";\\s*HttpOnly")
    }
  }

  test("cookie path defaults to context path") {
    post("/foo/setcookie", "cookieval" -> "whatever") {
      headers("Set-Cookie") must include.regex(";\\s*Path=/foo")
    }
  }

  test("cookie path defaults to context path when using a maplike setter") {
    post("/foo/maplikeset", "cookieval" -> "whatever") {
      val hdr = headers("Set-Cookie")
      hdr must startWith ("""somecookie=whatever;""")
      hdr must include.regex(";\\s*Path=/foo")
    }
  }

  test("cookie path defaults to '/' in root context") {
    post("/setcookie", "cookieval" -> "whatever") {
      val cookie = JHttpCookie.parse(headers("Set-Cookie")).get(0)
      cookie.getPath must be ("/")
    }
  }

  // This is as much a test of ScalatraTests as it is of CookieSupport.
  // http://github.com/scalatra/scalatra/issue/84
  test("handles multiple cookies") {
    session {
      post("/foo/setcookie", "cookieval" -> "The value", "anothercookieval" -> "Another Cookie") {
        body must equal("OK")
      }
      get("/foo/getcookie") {
        body must equal("The value")
        headers("X-Another-Cookie") must equal ("Another Cookie")
      }
    }
  }

  test("respects the HttpOnly option") {
    post("/foo/set-http-only-cookie", "cookieval" -> "whatever") {
      val hdr = headers("Set-Cookie")
      hdr must include.regex(";\\s*HttpOnly")
    }
  }

  test("removes a cookie by setting max-age = 0") {
    post("/foo/remove-cookie") {
      val hdr = headers("Set-Cookie")
      // Jetty turns Max-Age into Expires
      hdr must include.regex(";\\s*Max-Age=0")
    }
  }

  test("removes a cookie by setting a path") {
    post("/foo/remove-cookie-with-path") {
      val hdr = headers("Set-Cookie")
      // Jetty turns Max-Age into Expires
      hdr must include.regex(";\\s*Max-Age=0")
      hdr must include.regex(";\\s*Path=/bar")
    }
  }

  test("removing a cookie removes it from the map view") {
    session {
      post("/foo/setcookie") {}
      post("/foo/remove-cookie") {
        headers("Somecookie-Is-Defined") must be ("false")
      }
    }
  }


}

class NettyCookieSupportTest extends CookieSupportTest with NettyBackend