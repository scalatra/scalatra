package org.scalatra

import org.scalatra.test.scalatest.ScalatraFunSuite

class RequestCookiesTest extends ScalatraFunSuite {
  addServlet(
    new ScalatraServlet {
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
    },
    "/*"
  )

  test("multiCookies is a multi-map of names to values") {
    get("/multi-cookies", headers = Map("Cookie" -> "one=uno; one=eins; two=zwei")) {
      header("one") should equal("uno:eins")
      header("two") should equal("zwei")
      header("three") should equal("")
    }
  }

  test("cookies is a map of names to values") {
    get("/cookies", headers = Map("Cookie" -> "one=uno; one=eins; two=zwei")) {
      header("one") should equal("uno")
      header("two") should equal("zwei")
      header("three") should equal("NONE")
    }
  }
}
