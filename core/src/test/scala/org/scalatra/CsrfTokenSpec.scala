package org.scalatra

import org.scalatra.test.specs2.MutableScalatraSpec

class CsrfTokenServlet extends ScalatraServlet with CsrfTokenSupport {
  get("/renderForm") {
    <html>
      <body>
        <form method="post"><input type="hidden" name={ csrfKey } value={ csrfToken }/></form>
      </body>
    </html>
  }

  post("/renderForm") {
    "SUCCESS"
  }
}

object CsrfTokenSpec extends MutableScalatraSpec {

  addServlet(classOf[CsrfTokenServlet], "/*")

  "the get request should include the CSRF token" in {
    get("/renderForm") {
      body must beMatching("""(?s).*value="\w+".*""")
    }
  }

  "the post should be valid when it uses the right csrf token" in {
    var token = ""
    session {
      get("/renderForm") {
        token = ("value=\"(\\w+)\"".r findFirstMatchIn body).get.subgroups.head
      }
      post("/renderForm", CsrfTokenSupport.DefaultKey -> token) {
        body must be_==("SUCCESS")
      }
    }
  }

  "the post should be invalid when it uses a different csrf token" in {
    session {
      get("/renderForm") {
      }
      post("/renderForm", CsrfTokenSupport.DefaultKey -> "Hey I'm different") {
        status must be_==(403)
        body must not be_== ("SUCCESS")
      }
    }
  }

  "the token should remain valid across multiple request" in {
    var token = ""
    session {
      get("/renderForm") {
        token = ("value=\"(\\w+)\"".r findFirstMatchIn body).get.subgroups.head
      }
      get("/renderForm") {
        val token2 = ("value=\"(\\w+)\"".r findFirstMatchIn body).get.subgroups.head
        token must be_==(token2)
      }
      post("/renderForm", CsrfTokenSupport.DefaultKey -> token) {
        body must be_==("SUCCESS")
      }
    }

  }

}

// vim: set si ts=2 sw=2 sts=2 et:
