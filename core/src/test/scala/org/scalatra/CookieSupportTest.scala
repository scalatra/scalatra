package org.scalatra

import test.scalatest.ScalatraFunSuite
import org.scalatest.matchers.MustMatchers
import java.net.HttpCookie

class CookieSupportServlet extends ScalatraServlet with CookieSupport {

  get("/getcookie") {
    cookies.get("anothercookie") foreach { cookie =>
      response.setHeader("X-Another-Cookie", cookie)
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
    response.setHeader("Somecookie-Is-Defined",
      cookies.get("somecookie").isDefined.toString)
  }

  post("/remove-cookie-with-path") {
    cookies.delete("somecookie")(CookieOptions(path = "/bar"))
  }
}

class CookieSupportTest extends ScalatraFunSuite {
  val oneWeek = 7 * 24 * 3600

  tester.setContextPath("/foo")
  addServlet(classOf[CookieSupportServlet], "/*")

  test("GET /getcookie with no cookies set should return 'None'") {
    get("/foo/getcookie") {
      body must equal("None")
    }
  }

  test("POST /setcookie with a value should return OK") {
    post("/foo/setcookie", "cookieval" -> "The value") {
      response.getHeader("Set-Cookie") must startWith ("""somecookie="The value";""")
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
      val cookie = HttpCookie.parse(response.getHeader("Set-Cookie")).get(0)
      // Allow some slop, since it's a new call to currentTimeMillis
      cookie.getMaxAge.toInt must be (oneWeek plusOrMinus 10000)
    }
  }

  test("cookie path defaults to context path") {
    post("/foo/setcookie", "cookieval" -> "whatever") {
      response.getHeader("Set-Cookie") must include (";Path=/foo")
    }
  }

  test("cookie path defaults to context path when using a maplike setter") {
    post("/foo/maplikeset", "cookieval" -> "whatever") {
      val hdr = response.getHeader("Set-Cookie")
      hdr must startWith ("""somecookie=whatever;""")
      hdr must include (";Path=/foo")
    }
  }

  test("cookie path defaults to '/' in root context") {
    withContextPath("") {
      post("/setcookie", "cookieval" -> "whatever") {
        val cookie = HttpCookie.parse(response.getHeader("Set-Cookie")).get(0)
        cookie.getPath must be ("/")
      }
    }
  }

  // This is as much a test of ScalatraTests as it is of CookieSupport.
  // http://github.com/scalatra/scalatra/issue/84
  test("handles multiple cookies") {
    session {
      post("/foo/setcookie", Map("cookieval" -> "The value", "anothercookieval" -> "Another Cookie")) {
        body must equal("OK")
      }
      get("/foo/getcookie") {
        body must equal("The value")
        header("X-Another-Cookie") must equal ("Another Cookie")
      }
    }
  }

<<<<<<< HEAD
  test("respects the HttpOnly option") {
    post("/foo/set-http-only-cookie", "cookieval" -> "whatever") {
      val hdr = response.getHeader("Set-Cookie")
      hdr must include (";HttpOnly")
    }
  }

  test("removes a cookie by setting max-age = 0") {
    post("/foo/remove-cookie") {
      val hdr = response.getHeader("Set-Cookie")
      // Jetty turns Max-Age into Expires
      hdr must include (";Expires=Thu, 01-Jan-1970 00:00:00 GMT")
    }
  }

  test("removes a cookie by setting a path") {
    post("/foo/remove-cookie-with-path") {
      val hdr = response.getHeader("Set-Cookie")
      // Jetty turns Max-Age into Expires
      hdr must include (";Expires=Thu, 01-Jan-1970 00:00:00 GMT")
      hdr must include (";Path=/bar")
    }
  }

  test("removing a cookie removes it from the map view") {
    session {
      post("/foo/setcookie") {}
      post("/foo/remove-cookie") {
        header("Somecookie-Is-Defined") must be ("false")
      }
    }
  }

  def withContextPath[A](path: String)(f: => A): A = {
    val old = tester.getContext.getContextPath
    try {
      tester.setContextPath(path)
      f
    }
    finally {
      tester.setContextPath(old)
    }
  }
}
