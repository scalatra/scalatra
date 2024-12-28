package org.scalatra

import org.scalatra.test.scalatest.ScalatraFunSuite

class SessionTestServlet extends ScalatraServlet {
  get("/session") {
    session.getOrElse("val", "None")
  }

  post("/session") {
    session("val") = params("val")
    session.getOrElse("val", "None")
  }

  get("/session-option") {
    sessionOption map { _ => "Some" } getOrElse "None"
  }

}

class SessionTest extends ScalatraFunSuite {
  addServlet(classOf[SessionTestServlet], "/*")

  test("GET /session with no session should return 'None'") {
    get("/session") {
      body should equal("None")
    }
  }

  test("POST /session with val=yes should return 'yes'") {
    post("/session", "val" -> "yes") {
      body should equal("yes")
    }
  }

  test(
    "GET /session with the session should return the data set in POST /session"
  ) {
    val data = "some data going in as symbol"
    session {
      post("/session", "val" -> data) {
        body should equal(data)
      }
      get("/session") {
        body should equal(data)
      }
    }
  }

  test("sessionOption should be None when no session exists") {
    session {
      get("/session-option") {
        body should equal("None")
      }
    }
  }

  test("sessionOption should be Some when a session is active") {
    session {
      post("/session") { () }

      get("/session-option") {
        body should equal("Some")
      }
    }
  }
}
