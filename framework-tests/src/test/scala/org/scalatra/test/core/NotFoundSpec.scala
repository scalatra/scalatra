package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.specs2.ScalatraSpec

abstract class NotFoundSpec extends ScalatraSpec { def is =
  "The notFound block"                          ^
    "should run when no route matches"          ! customNotFound^
    "should not result in a 404 if the block doesn't" ! customNotFoundStatus^
    "in ScalatraServlet"                        ^
      "should send a 404"                       ! servletNotFoundSends404 ^ bt ^ bt ^
//    "in ScalatraFilter"                         ^
//      "should invoke the chain"                 ! filterNotFoundInvokesChain^
//                                                end^
  "The methodNotAllowed block"                  ^
    "should run when a route matches other methods" ! customMethodNotAllowed^
    "should support pass"                       ! passFromNotAllowed^
    "by default"                                ^
      "should send a 405"                       ! defaultMethodNotAllowedSends405^
      "should set the allow header"             ! allowHeader^
      "HEAD should be implied by GET"           ! getImpliesHead^
    "should pass in a filter by default"        ! methodNotAllowedFilterPass^
                                                end
//
//  addFilter(new ScalatraFilter {
//    post("/filtered/get") { "wrong method" }
//  }, "/filtered/*")
//
  mount("/filtered", new ScalatraApp {
    get("/fall-through") { "fell through" }
    get("/get") { "servlet get" }
  })

  mount("/default", new ScalatraApp {
    get("/get") { "foo" }
    post("/no-get") { "foo" }
    put("/no-get") { "foo" }
  })

  mount("/custom", new ScalatraApp {
    post("/no-get") { "foo" }
    put("/no-get") { "foo" }

    notFound { "custom not found" }
    methodNotAllowed { _ => "custom method not allowed" }
  })

  mount("/pass-from-not-allowed", new ScalatraApp {
    post("/no-get") { "foo" }
    notFound { "fell through" }
    methodNotAllowed { _ => pass() }
  })

  def customNotFound = {
    get("/custom/matches-nothing") {
      body must_== "custom not found"
    }
  }

  def customNotFoundStatus = get("/custom/matches-nothing") {
    status.code must_== 200
  }

  def servletNotFoundSends404 = get("/default/matches-nothing") {
    status.code must_== 404
  }

  def filterNotFoundInvokesChain = get("/filtered/fall-through") {
    body must_== "fell through"
  }

  def customMethodNotAllowed = get("/custom/no-get") {
    body must_== "custom method not allowed"
  }

  def defaultMethodNotAllowedSends405 = get("/default/no-get") {
    status.code must_== 405
  }

  def allowHeader = get("/default/no-get") {
    headers("Allow").split(", ").toSet must_== Set("POST", "PUT")
  }

  def getImpliesHead = post("/default/get") {
    headers("Allow").split(", ").toSet must_== Set("GET", "HEAD")
  }

  def passFromNotAllowed = get("/pass-from-not-allowed/no-get") {
    body must_== "fell through"
  }

  def methodNotAllowedFilterPass = get("/filtered/get") {
    body must_== "servlet get"
  }
}

class NettyNotFoundSpec extends NotFoundSpec with NettyBackend
class JettyNotFoundSpec extends NotFoundSpec with JettyBackend