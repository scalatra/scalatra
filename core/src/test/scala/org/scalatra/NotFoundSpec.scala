package org.scalatra

import org.scalatra.test.scalatest.ScalatraWordSpec

class NotFoundSpec extends ScalatraWordSpec {
  "The notFound block" should {
    "run when no route matches" in {
      get("/custom/matches-nothing") {
        assert(body == "custom not found")
      }
    }
    "not result in a 404 if the block doesn't" in {
      get("/custom/matches-nothing") {
        assert(status == 200)
      }
    }
    "ScalatraServlet" should {
      "send a 404" in {
        get("/default/matches-nothing") {
          assert(status == 404)
        }
      }
    }
    "ScalatraFilter" should {
      "should invoke the chain" in {
        get("/filtered/fall-through") {
          assert(body == "fell through")
        }
      }
    }
  }

  "The methodNotAllowed block" should {
    "run when a route matches other methods" in {
      get("/custom/no-get") {
        assert(body == "custom method not allowed")
      }
    }
    "support pass" in {
      get("/pass-from-not-allowed/no-get") {
        assert(body == "fell through")
      }
    }
    "by default" should {
      "send a 405 " in {
        get("/default/no-get") {
          assert(status == 405)
        }
      }
      "set the allow header" in {
        get("/default/no-get") {
          assert(header("Allow").split(", ").toSet == Set("POST", "PUT"))
        }
      }
    }
    "HEAD should be implied by GET" should {
      "pass in a filter by default" in {
        get("/filtered/get") {
          assert(body == "servlet get")
        }
      }
    }
  }

  addFilter(new ScalatraFilter {
    post("/filtered/get") { "wrong method" }
  }, "/filtered/*")

  addServlet(new ScalatraServlet {
    get("/fall-through") { "fell through" }
    get("/get") { "servlet get" }
  }, "/filtered/*")

  addServlet(new ScalatraServlet {
    get("/get") { "foo" }
    post("/no-get") { "foo" }
    put("/no-get") { "foo" }

    error {
      case t: Throwable => t.printStackTrace()
    }
  }, "/default/*")

  addServlet(new ScalatraServlet {
    post("/no-get") { "foo" }
    put("/no-get") { "foo" }

    notFound { "custom not found" }
    methodNotAllowed { _ => "custom method not allowed" }
  }, "/custom/*")

  addServlet(new ScalatraServlet {
    post("/no-get") { "foo" }
    notFound { "fell through" }
    methodNotAllowed { _ => pass() }
  }, "/pass-from-not-allowed/*")

}
