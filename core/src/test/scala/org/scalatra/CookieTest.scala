package org.scalatra

import org.scalatest.matchers.MustMatchers
import org.scalatest._
import java.util.Date

class CookieTest extends WordSpec with Matchers with BeforeAndAfterAll {

  "a Cookie" should {

    "render a simple name value pair" in {
      val cookie = Cookie("theName", "theValue")
      cookie.toCookieString should equal("theName=theValue")
    }

    "have a dot in front of the domain when set" in {
      val cookie = Cookie("cookiename", "value1")(CookieOptions(domain = "nowhere.com"))
      cookie.toCookieString should equal("cookiename=value1; Domain=.nowhere.com")
    }

    "prefix a path with / if a path is set" in {
      val cookie = Cookie("cookiename", "value1")(CookieOptions(path = "path/to/resource"))
      cookie.toCookieString should equal("cookiename=value1; Path=/path/to/resource")
    }

    "have a maxAge when the value is >= 0" in {
      val cookie = Cookie("cookiename", "value1")(CookieOptions(maxAge = 86700))
      val dateString = Cookie.formatExpires(new Date(Cookie.currentTimeMillis + 86700000))
      cookie.toCookieString should equal("cookiename=value1; Expires=" + dateString)
    }

    "set the comment when a comment is given" in {
      val cookie = Cookie("cookiename", "value1")(CookieOptions(comment = "This is the comment"))
      cookie.toCookieString should equal("cookiename=value1; Comment=This is the comment")
    }

    "flag the cookie as secure if needed" in {
      val cookie = Cookie("cookiename", "value1")(CookieOptions(secure = true))
      cookie.toCookieString should equal("cookiename=value1; Secure")
    }

    "flag the cookie as http only if needed" in {
      val cookie = Cookie("cookiename", "value1")(CookieOptions(httpOnly = true))
      cookie.toCookieString should equal("cookiename=value1; HttpOnly")
    }

    "render a cookie with all options set" in {
      val cookie = Cookie("cookiename", "value3")(CookieOptions(
        domain = "nowhere.com",
        path = "path/to/page",
        comment = "the cookie thingy comment",
        maxAge = 15500,
        secure = true,
        httpOnly = true
      ))
      val d = new Date(Cookie.currentTimeMillis + 15500 * 1000)
      cookie.toCookieString should
        equal("cookiename=value3; Domain=.nowhere.com; Path=/path/to/page; Comment=the cookie thingy comment; " +
          "Expires=" + Cookie.formatExpires(d) + "; Secure; HttpOnly")
    }
  }

  override protected def afterAll() {
    Cookie.unfreezeTime()
  }

  override protected def beforeAll() {
    Cookie.freezeTime()
  }
}
