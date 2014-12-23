package org.scalatra

import java.net.HttpCookie

import org.scalatra.test.specs2.MutableScalatraSpec

import scala.collection.JavaConverters._

class XsrfTokenServlet extends ScalatraServlet with XsrfTokenSupport {

  xsrfGuard()

  get("/renderForm") {
    "GO"
  }

  post("/renderForm") {
    "SUCCESS"
  }
}

object XsrfTokenSpec extends MutableScalatraSpec {

  addServlet(classOf[XsrfTokenServlet], "/*")

  def tokenFromCookie = {
    response.getHeaderValues("Set-Cookie").asScala.flatMap { s =>
      HttpCookie.parse(s).asScala.toList
    }.find(_.getName == "XSRF-TOKEN").map(_.getValue).getOrElse("")
  }

  "the get request should include the CSRF token" in {
    get("/renderForm") {
      tokenFromCookie must not be null
      tokenFromCookie must not be empty
      body must beMatching("GO")
    }
  }

  "the post should be valid when it uses the right csrf token" in {
    var token = ""
    session {
      get("/renderForm") {
        token = tokenFromCookie
        body must beMatching("GO")
      }
      post("/renderForm", headers = Map(XsrfTokenSupport.HeaderNames.head -> token)) {
        body must be_==("SUCCESS")
      }
    }
  }

  "the post should be invalid when it uses a different csrf token" in {
    session {
      get("/renderForm") {
        body must beMatching("GO")
      }
      post("/renderForm", headers = Map(XsrfTokenSupport.HeaderNames.head -> "Hey I'm different")) {
        status must be_==(403)
        body must not be_== ("SUCCESS")
      }
    }
  }

  "the token should remain valid across multiple request" in {
    var token = ""
    session {
      get("/renderForm") {
        token = tokenFromCookie
        body must beMatching("GO")
      }
      get("/renderForm") {
        body must beMatching("GO")
      }
      post("/renderForm", headers = Map(XsrfTokenSupport.HeaderNames.head -> token)) {
        body must be_==("SUCCESS")
      }
      post("/renderForm", headers = Map(XsrfTokenSupport.HeaderNames.head -> token)) {
        body must be_==("SUCCESS")
      }
    }

  }

}

// vim: set si ts=2 sw=2 sts=2 et:
