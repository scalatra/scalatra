package org.scalatra

import org.scalatest._

class ScalatraServletRequestPathSpec extends WordSpec with MustMatchers {

  "a ScalatraServlet requestPath" should {

    "be extracted properly when encoded url contains semicolon" in {
      ScalatraServlet.requestPath("/test%3Btest/", 0) must equal("/test;test/")
    }

    "be extracted properly when url contains semicolon" in {
      ScalatraServlet.requestPath("/test;test/", 0) must equal("/test")
    }

    "be extracted properly from encoded url" in {
      ScalatraServlet.requestPath("/%D1%82%D0%B5%D1%81%D1%82/", 5) must equal("/")
      ScalatraServlet.requestPath("/%D1%82%D0%B5%D1%81%D1%82/%D1%82%D0%B5%D1%81%D1%82/", 5) must equal("/тест/")
    }

    "be extracted properly from decoded url" in {
      ScalatraServlet.requestPath("/тест/", 5) must equal("/")
      ScalatraServlet.requestPath("/тест/тест/", 5) must equal("/тест/")
    }
  }
}
