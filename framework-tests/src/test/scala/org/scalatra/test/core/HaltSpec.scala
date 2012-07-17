package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.specs2.ScalatraSpec

class HaltTestApp extends ScalatraApp {
  before() {
    status = 501
    response.headers += "Before-Header" -> "before"
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
    halt(403, <h1>Go away</h1>)
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

  after() {
    response.headers += "After-Header" -> "after"
  }
}

abstract class HaltSpec extends ScalatraSpec { def is =
  "halt with no arguments should"               ^
    "behave like a common halt"                 ^ commonHalt("/no-args")^
    "not alter the status"                      ! status("/no-args", 501)^
    "retain the headers"                        ! retainsHeaders("/no-args")^
                                                end^
  "halt with only a status should"              ^
    "behave like a common halt"                 ^ commonHalt("/status")^
    "set the status"                            ! status("/status-and-reason", 404)^
                                                end^
  "halt with only a body should"                ^
    "behave like a common halt"                 ^ commonHalt("/body")^
    "not alter the status"                      ! status("/body", 501)^
    "render the body"                           ! bodyEquals("/body", "<h1>Go away</h1>")^
                                                end^
  "halt with a status and body should"          ^
    "behave like a common halt"                 ^ commonHalt("/status-and-body")^
    "set the status"                            ! status("/status-and-body", 403)^
    "render the body"                           ! bodyEquals("/status-and-body", "<h1>Go away</h1>")^
                                                end^
  "halt with all arguments should"              ^
    "behave like a common halt"                 ^ commonHalt("/all-args")^
    "set the status"                            ! status("/all-args", 403)^
    "set the reason"                            ! reason("/all-args", "Go away")^
    "set the headers"                           ! hasHeader("/all-args", "X-Your-Father-Smelt-Of", "elderberries")^
    "render the body"                           ! bodyEquals("/all-args", "<h1>Go away or I shall taunt you a second time!</h1>")^
                                                end^
  "halt in a before filter should"              ^
    "behave like a common halt"                 ^ commonHalt("/halt-before?haltBefore=true")

  mount(new HaltTestApp)

  def commonHalt(uri: String) =
    "not execute the rest of the action"        ! haltsAction(uri)^
    "retain the headers"                        ! retainsHeaders(uri)^
    "still execute after filter"                ! hasHeader(uri, "After-Header", "after")

  def haltsAction(uri: String) =
    get(uri) {
      body must not contain ("this content must not be returned")
    }

  def status(uri: String, status: Int) =
    get(uri) {
      response.statusCode must_== status
    }

  def retainsHeaders(uri: String) = hasHeader(uri, "Before-Header", "before")

  def reason(uri: String, reason: String) =
    get(uri) {
      response.status.message must_== reason
    }

  def bodyContains(uri: String, text: String) =
    get(uri) {
      body must contain (text)
    }

  def bodyEquals(uri: String, text: String) =
    get(uri) {
      body must_== (text)
    }

  def hasHeader(uri: String, name: String, value: String) =
    get(uri) {
      headers.get(name) must beSome(value)
    }
}

class NettyHaltSpec extends HaltSpec with NettyBackend
class JettyHaltSpec extends HaltSpec with JettyBackend
