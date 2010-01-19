package com.thinkminimo.step

import org.scalatest.matchers.ShouldMatchers

class SessionTestServlet extends Step {
  get("/session") {
    session("val") match {
      case Some(v:String) => v
      case _ => "None"
    }
  }

  post("/session") {
    session("val") = params("val")
    session("val") match {
      case Some(v:String) => v
      case _ => "None"
    }
  }
}

class SessionTest extends StepSuite with ShouldMatchers {
  route(classOf[SessionTestServlet], "/*")

  test("GET /session with no session should return 'None'") {
    get("/session") {
      body should equal ("None")
    }
  }

  test("POST /session with val=yes should return 'yes'") {
    post("/session", "val" -> "yes") {
      body should equal ("yes")
    }
  }

  test("GET /session with the session should return the data set in POST /session") {
    val data = "session_value"
    session {
      post("/session", "val" -> data) {
        body should equal (data)
      }
      get("/session") {
        body should equal (data)        
      }
    }
  }
}

