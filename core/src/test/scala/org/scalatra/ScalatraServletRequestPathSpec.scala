package org.scalatra

import org.scalatest._

class ScalatraServletRequestPathSpec extends WordSpec with MustMatchers {

  "a ScalatraServlet requestPath" should {

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
