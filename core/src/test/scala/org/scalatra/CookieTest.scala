package org.scalatra

import org.specs2.Specification

class CookieSpec extends Specification { def is =

  "A Cookie should" ^
    "render a simple name value pair" ! simpleNameValuePair ^
    "have a dot in front of the domain when set" ! hasDotInFrontOfDomain ^
    "prefix a path with / if a path is set" ! prefixesPathWithSlash ^
    "have a maxAge when the value is >= 0" ! hasMaxAge ^
    "set the comment when a comment is given" ! setsComment ^
    "flag the cookie as secure if needed" ! flagsSecure ^
    "flag the cookie as http only if needed" ! flagsHttp ^
    "render a cookie with all supported options set" ! fullOption ^
  end

  def simpleNameValuePair = {
    val cookie = Cookie("theName", "theValue")
    cookie.toCookieString must_== "theName=theValue"
  }

  def hasDotInFrontOfDomain = {
    val cookie = Cookie("cookiename", "value1")( CookieOptions(domain="nowhere.com"))
    cookie.toCookieString must_== "cookiename=value1; Domain=.nowhere.com"
  }

  def prefixesPathWithSlash = {
    val cookie = Cookie("cookiename", "value1")( CookieOptions(path="path/to/resource"))
    cookie.toCookieString must_==  "cookiename=value1; Path=/path/to/resource"
  }

  def hasMaxAge = {
    val cookie = Cookie("cookiename", "value1")(CookieOptions(maxAge=86700))
    cookie.toCookieString must_==  "cookiename=value1; Max-Age=86700"
  }

  def setsComment = {
    val cookie = Cookie("cookiename", "value1")(CookieOptions(comment="This is the comment"))
    cookie.toCookieString must_== "cookiename=value1; Comment=This is the comment"
  }

  def flagsSecure = {
    val cookie = Cookie("cookiename", "value1")(CookieOptions(secure = true))
    cookie.toCookieString must_== "cookiename=value1; Secure"
  }

  def flagsHttp = {
    val cookie = Cookie("cookiename", "value1")( CookieOptions(httpOnly = true))
    cookie.toCookieString must_== "cookiename=value1; HttpOnly"
  }

  def fullOption = {
    val cookie = Cookie("cookiename", "value3")(CookieOptions(
      domain="nowhere.com",
      path="path/to/page",
      comment="the cookie thingy comment",
      maxAge=15500,
      secure=true,
      httpOnly=true
    ))
    cookie.toCookieString must_==
     "cookiename=value3; Domain=.nowhere.com; Path=/path/to/page; Comment=the cookie thingy comment; " +
             "Max-Age=15500; Secure; HttpOnly"
  }

}
