package org.scalatra

import org.scalatra.test.specs2.MutableScalatraSpec

class CsrfTokenServlet extends ScalatraServlet with CsrfTokenSupport {
  get("/renderForm") {
    <html>
      <body>
        <form method="post"><input type="hidden" name={csrfKey} value={csrfToken}/></form>
      </body>
    </html>
  }

  post("/renderForm") {
    "SUCCESS"
  }

  get("/plain") {
    "PLAIN"
  }

  get("/sessionWithoutToken") {
    session("other") = "value"
    "SESSION"
  }

  post("/signin") {
    resetCsrfToken()
    csrfToken
  }
}

class EagerCsrfTokenServlet extends ScalatraServlet with CsrfTokenSupport {
  override protected def csrfEagerToken: Boolean = true

  get("/plain") {
    "PLAIN"
  }
}

class AuthorizationCsrfTokenServlet extends ScalatraServlet with CsrfTokenSupport {
  override protected def csrfCredentialHeaders: Seq[String] = CsrfTokenSupport.CredentialHeaders :+ "Authorization"

  post("/") {
    "SUCCESS"
  }
}

object CsrfTokenSpec extends MutableScalatraSpec {

  addServlet(classOf[CsrfTokenServlet], "/*")
  addServlet(classOf[EagerCsrfTokenServlet], "/eager/*")
  addServlet(classOf[AuthorizationCsrfTokenServlet], "/authorization/*")

  private def tokenIn(body: String): String = ("value=\"(\\w+)\"".r findFirstMatchIn body).get.group(1)

  private def setsCookie: Boolean = response.headers.keys.exists(_.equalsIgnoreCase("Set-Cookie"))

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
        ()
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

  "a get that doesn't read the token should create no session" in {
    get("/plain") {
      body must be_==("PLAIN")
      setsCookie must beFalse
    }
  }

  "a get that renders the token should create the session" in {
    get("/renderForm") {
      setsCookie must beTrue
    }
  }

  "a post without a cookie should not be checked and create no session" in {
    post("/renderForm", "other" -> "value") {
      body must be_==("SUCCESS")
      setsCookie must beFalse
    }
  }

  "the post should be valid when it sends the right csrf token in the header" in {
    var token = ""
    session {
      get("/renderForm") {
        token = tokenIn(body)
      }
      post("/renderForm", Seq("other" -> "value"), Map("X-CSRF-TOKEN" -> token)) {
        body must be_==("SUCCESS")
      }
    }
  }

  "the post should be invalid when it sends no csrf token" in {
    session {
      get("/renderForm") {
        ()
      }
      post("/renderForm", "other" -> "value") {
        status must be_==(403)
      }
    }
  }

  "the post should be invalid when it sends an empty csrf token" in {
    session {
      get("/renderForm") {
        ()
      }
      post("/renderForm", CsrfTokenSupport.DefaultKey -> "") {
        status must be_==(403)
      }
    }
  }

  "a post with a cookie should be invalid when there is no session" in {
    post("/renderForm", Seq(CsrfTokenSupport.DefaultKey -> "guessed"), Map("Cookie" -> "other=value")) {
      status must be_==(403)
    }
  }

  "a post with a cookie should be invalid when the session has no token" in {
    session {
      get("/sessionWithoutToken") {
        setsCookie must beTrue
      }
      post("/renderForm", "other" -> "value") {
        status must be_==(403)
      }
    }
  }

  "resetCsrfToken should replace the token" in {
    var token    = ""
    var newToken = ""
    session {
      get("/renderForm") {
        token = tokenIn(body)
      }
      post("/signin", CsrfTokenSupport.DefaultKey -> token) {
        status must be_==(200)
        newToken = body
      }
      newToken must not be_== (token)
      post("/renderForm", CsrfTokenSupport.DefaultKey -> token) {
        status must be_==(403)
      }
      post("/renderForm", CsrfTokenSupport.DefaultKey -> newToken) {
        body must be_==("SUCCESS")
      }
    }
  }

  "csrfEagerToken should create the session and the token on every request" in {
    get("/eager/plain") {
      body must be_==("PLAIN")
      setsCookie must beTrue
    }
  }

  "an Authorization header should make a post subject to the check if configured" in {
    post("/authorization/", Seq("other" -> "value"), Map("Authorization" -> "Basic dXNlcjpwYXNz")) {
      status must be_==(403)
    }
  }

  "a post without a credential header should not be checked if Authorization is configured" in {
    post("/authorization/", "other" -> "value") {
      body must be_==("SUCCESS")
    }
  }

}

// vim: set si ts=2 sw=2 sts=2 et:
