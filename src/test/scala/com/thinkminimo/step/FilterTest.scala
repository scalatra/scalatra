package com.thinkminimo.step

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers

class FilterTestServlet extends Step {
  var beforeCount = 0

  before { 
    beforeCount += 1
    params.get("body") match {
      case Some(x) => response.getWriter.write(x)
      case None => 
    }
  }
  
  get("/") {}
  
  get("/counter") { beforeCount }
  
  post("/reset-before-counter") { beforeCount = 0 }
}

class MultipleBeforeFilterTestServlet extends Step {
  before {
    response.getWriter.print("one\n")
  }

  before {
    response.getWriter.print("two\n")
  }

  get("/") {}
}

class FilterTest extends StepSuite with BeforeAndAfterEach with ShouldMatchers {
  route(classOf[FilterTestServlet], "/*")
  route(classOf[MultipleBeforeFilterTestServlet], "/multiple-before-filters/*")
  
  override def beforeEach() {
	post("/reset-before-counter") {}
  }
  
  test("before is called exactly once per request") {
    get("/counter") { body should equal("1") }
    get("/counter") { body should equal("2") }
  }
  
  test("before is called when route is not found") {
    get("/this-route-does-not-exist") {
      // Should be 1, but we can't see it yet
    }
    get("/counter") { 
     // Should now be 2.  1 for the last request, and one for this
     body should equal ("2")
    }
  }

  test("before can see query parameters") {
    get("/", "body" -> "foo") {
      body should equal ("foo")
    }
  }

  test("supports multiple before filters") {
    get("/multiple-before-filters/") {
      body should equal ("one\ntwo\n")
    }
  }
}

