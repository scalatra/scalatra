package org.scalatra

import org.scalatest.BeforeAndAfterEach
import test.scalatest.ScalatraFunSuite
import javax.servlet.http.{ HttpServletResponse, HttpServletRequest }

class ScalatraExpectedFilterException extends RuntimeException
class FilterTestServlet extends ScalatraServlet {
  var beforeCount = 0
  var afterCount = 0

  before() {
    beforeCount += 1
    params.get("before") match {
      case Some(x) => response.getWriter.write(x)
      case None =>
    }
  }

  after() {
    afterCount += 1
    params.get("after") match {
      case Some(x) =>
        response.getWriter.write(x)
      case None =>
    }
  }

  get("/") {}

  get("/before-counter") {
    beforeCount.toString
  }

  get("/after-counter") {
    afterCount.toString
  }

  get("/demons-be-here") {
    throw new ScalatraExpectedFilterException
  }

  post("/reset-counters") {
    beforeCount = 0
    afterCount = 0
  }
}

// Ugh... what should we call this?  Sinatra calls before/after "filter", which is not related to a
// javax.servlet.Filter.
class FilterTestFilter extends ScalatraFilter {
  var beforeCount = 0

  before() {
    beforeCount += 1
    response.setHeader("filterBeforeCount", beforeCount.toString)
  }

  post("/reset-counters") {
    beforeCount = 0
    pass
  }

}

class MultipleFilterTestServlet extends ScalatraServlet {
  before() {
    response.writer.print("one\n")
  }

  before() {
    response.writer.print("two\n")
  }

  get("/") {
    response.writer.print("three\n")
  }

  after() {
    response.writer.print("four\n")
  }

  after() {
    response.writer.print("five\n")
  }

}

class FilterTest extends ScalatraFunSuite with BeforeAndAfterEach {
  addServlet(classOf[FilterTestServlet], "/*")
  addServlet(classOf[MultipleFilterTestServlet], "/multiple-filters/*")
  addFilter(classOf[FilterTestFilter], "/*")

  override def beforeEach() {
    post("/reset-counters") {}
  }

  test("before is called exactly once per request to a servlet") {
    get("/before-counter") { body should equal("1") }
    get("/before-counter") { body should equal("2") }
  }

  test("before is called exactly once per request to a filter") {
    get("/before-counter") { header("filterBeforeCount") should equal("1") }
    get("/before-counter") { header("filterBeforeCount") should equal("2") }
  }

  test("before is called when route is not found") {
    get("/this-route-does-not-exist") {
      // Should be 1, but we can't see it yet
    }
    get("/before-counter") {
      // Should now be 2.  1 for the last request, and one for this
      body should equal("2")
    }
  }

  test("before can see query parameters") {
    get("/", "before" -> "foo") {
      body should equal("foo")
    }
  }

  test("supports multiple before and after filters") {
    get("/multiple-filters/") {
      body should equal("one\ntwo\nthree\nfour\nfive\n")
    }
  }

  test("after is called exactly once per request") {
    get("/after-counter") { body should equal("1") }
    get("/after-counter") { body should equal("2") }
  }

  test("after is called when route is not found") {
    get("/this-route-does-not-exist") {
      // Should be 1, but we can't see it yet
    }
    get("/after-counter") {
      // Should now be 2.  1 for the last request, and one for this
      body should equal("2")
    }
  }

  test("after can see query parameters") {
    get("/", "after" -> "foo") {
      body should equal("foo")
    }
  }

  test("after is called even if route handler throws exception") {
    get("/demons-be-here") {}
    get("/after-counter") {
      body should equal("2")
    }
  }
}

