package org.scalatra

import javax.servlet.http.HttpServletResponse
import test.specs2.ScalatraSpec

class HaltTestServlet extends ScalatraServlet {
  before() {
    status = 501
    response.setHeader("Before-Header", "before")
    if (params.isDefinedAt("haltBefore")) {
      halt(503)
    }
  }

  get("/no-args") {
    halt()
    "this content must not be returned"
  }

  get("/status") {
    halt(403)
    "this content must not be returned"
  }

  get("/body") {
    halt(body = <h1>Go away</h1>)
    "this content must not be returned"
  }

  get("/status-and-body") {
    halt(404, <h1>Not Here</h1>)
    "this content must not be returned"
  }

  get("/all-args") {
    halt(status = 403,
      reason = "Go away",
      headers = Map("X-Your-Mother-Was-A" -> "hamster", "X-Your-Father-Smelt-Of" -> "elderberries"),
      body = <h1>Go away or I shall taunt you a second time!</h1>)
    "this content must not be returned"
  }

  get("/halt-before") {
    "this content must not be returned"
  }

  get("/action-result") {
    halt(ActionResult(status = new ResponseStatus(406, "Not Acceptable"),
      headers = Map("X-Action-Result" -> "present"),
      body = "body sent using ActionResult"))
    "this content must not be returned"
  }

  after() {
    response.setHeader("After-Header", "after")
  }
}

class HaltSpec extends ScalatraSpec {
  def is =
    "halt with no arguments should" ^
      "behave like a common halt" ^ commonHalt("/no-args") ^
      "not alter the status" ! status("/no-args", 501) ^
      "retain the headers" ! retainsHeaders("/no-args") ^
      end ^
      "halt with only a status should" ^
      "behave like a common halt" ^ commonHalt("/status") ^
      "set the status" ! status("/status-and-reason", 403) ^
      end ^
      "halt with only a body should" ^
      "behave like a common halt" ^ commonHalt("/body") ^
      "not alter the status" ! status("/body", 501) ^
      "render the body" ! bodyEquals("/body", "<h1>Go away</h1>") ^
      end ^
      "halt with a status and body should" ^
      "behave like a common halt" ^ commonHalt("/status-and-body") ^
      "set the status" ! status("/status-and-body", 404) ^
      "render the body" ! bodyEquals("/status-and-body", "<h1>Not Here</h1>") ^
      end ^
      "halt with all arguments should" ^
      "behave like a common halt" ^ commonHalt("/all-args") ^
      "set the status" ! status("/all-args", 403) ^
      "set the reason" ! reason("/all-args", "Go away") ^
      "set the headers" ! hasHeader("/all-args", "X-Your-Father-Smelt-Of", "elderberries") ^
      "render the body" ! bodyEquals("/all-args", "<h1>Go away or I shall taunt you a second time!</h1>") ^
      end ^
      "halt with an ActionResult should" ^
      "behave like a common halt" ^ commonHalt("/action-result") ^
      "set the status" ! status("/action-result", 406) ^
      "set the reason" ! reason("/action-result", "Not Acceptable") ^
      "set the headers" ! hasHeader("/action-result", "X-Action-Result", "present") ^
      "render the body" ! bodyEquals("/action-result", "body sent using ActionResult") ^
      end ^
      "halt in a before filter should" ^
      "behave like a common halt" ^ commonHalt("/halt-before?haltBefore=true")

  addServlet(classOf[HaltTestServlet], "/*")

  def commonHalt(uri: String) =
    "not execute the rest of the action" ! haltsAction(uri) ^
      "retain the headers" ! retainsHeaders(uri) ^
      "still execute after filter" ! hasHeader(uri, "After-Header", "after")

  def haltsAction(uri: String) =
    get(uri) {
      body must not contain ("this content must not be returned")
    }

  def status(uri: String, status: Int) =
    get(uri) {
      status must_== status
    }

  def retainsHeaders(uri: String) = hasHeader(uri, "Before-Header", "before")

  def reason(uri: String, reason: String) =
    get(uri) {
      response.getReason must_== reason
    }

  def bodyContains(uri: String, text: String) =
    get(uri) {
      body must contain(text)
    }

  def bodyEquals(uri: String, text: String) =
    get(uri) {
      body must_== (text)
    }

  def hasHeader(uri: String, name: String, value: String) =
    get(uri) {
      header(name) must_== value
    }
}
