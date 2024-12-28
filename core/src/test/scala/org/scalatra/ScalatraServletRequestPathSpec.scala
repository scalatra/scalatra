package org.scalatra

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScalatraServletRequestPathSpec extends AnyWordSpec with Matchers {

  "a requestPath" should {

    val servlet = new ScalatraServlet {}

    "be extracted properly when encoded url contains semicolon" in {
      servlet.requestPath("/test%3Btest/", 0) must equal("/test;test/")
    }

    "be extracted properly when url contains semicolon" in {
      servlet.requestPath("/test;test/", 0) must equal("/test")
    }

    "be extracted properly from encoded url" in {
      servlet.requestPath("/%D1%82%D0%B5%D1%81%D1%82/", 5) must equal("/")
      servlet.requestPath(
        "/%D1%82%D0%B5%D1%81%D1%82/%D1%82%D0%B5%D1%81%D1%82/",
        5
      ) must equal("/тест/")
    }

    "be extracted properly from decoded url" in {
      servlet.requestPath("/тест/", 5) must equal("/")
      servlet.requestPath("/тест/тест/", 5) must equal("/тест/")
    }
  }
}
