package org.scalatra

import javax.servlet.http.{Cookie => ServletCookie}
import test.scalatest.ScalatraFunSuite
import org.scalatest.matchers.ShouldMatchers
import org.mortbay.jetty.testing.HttpTester

class RequestCookiesTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(new ScalatraServlet {
    get("/multi-cookies") {
      Seq("one", "two", "three") map { key =>
        response.setHeader(key, request.multiCookies(key).mkString(":"))
      }
    }

    get("/cookies") {
      Seq("one", "two", "three") map { key =>
        response.setHeader(key, request.cookies.getOrElse(key, "NONE"))
      }
    }
  }, "/*")

  test("multiCookies is a multi-map of names to values") {
    val req = new HttpTester
    req.setMethod("GET")
    req.setURI("/multi-cookies")
    req.setVersion("HTTP/1.0")
    req.addHeader("Cookie", "one=uno")
    req.addHeader("Cookie", "one=eins")
    req.addHeader("Cookie", "two=zwei")

    val res = new HttpTester
    res.parse(tester.getResponses(req.generate))
    res.getHeader("one") should be ("uno:eins")
    res.getHeader("two") should be ("zwei")
    res.getHeader("three") should be ("")
  }

  test("cookies is a map of names to values") {
    val req = new HttpTester
    req.setMethod("GET")
    req.setURI("/cookies")
    req.setVersion("HTTP/1.0")
    req.addHeader("Cookie", "one=uno")
    req.addHeader("Cookie", "one=eins")
    req.addHeader("Cookie", "two=zwei")

    val res = new HttpTester
    res.parse(tester.getResponses(req.generate))
    res.getHeader("one") should be ("uno")
    res.getHeader("two") should be ("zwei")
    res.getHeader("three") should be ("NONE")
  }
}
