package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.specs.ScalatraSpecification

class CsrfTokenServlet extends ScalatraApp with SessionSupport with CsrfTokenSupport {
  get("/renderForm") {
    <html>
      <body>
        <form method="post"><input type="hidden" name={csrfKey} value={csrfToken} /></form>
      </body>
    </html>
  }

  post("/renderForm") {
    "SUCCESS"
  }
}

abstract class CsrfTokenSpec extends ScalatraSpecification {

  mount(new CsrfTokenServlet)


  "the get request should include the CSRF token" in {
    get("/renderForm") {
      body must beMatching("""value="\w+""")
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
        status.code must be_==(403)
        body mustNot be_==("SUCCESS")
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


class NettyCsrfTokenSpec extends CsrfTokenSpec with NettyBackend
class JettyCsrfTokenSpec extends CsrfTokenSpec with JettyBackend
// vim: set si ts=2 sw=2 sts=2 et:
