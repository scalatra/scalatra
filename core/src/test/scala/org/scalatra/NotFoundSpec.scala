package org.scalatra

import specs2.ScalatraSpec

class NotFoundSpec extends ScalatraSpec { def is =
  "The notFound block"                          ^
    "should run when no route matches"          ! customNotFound^
    "in ScalatraServlet"                        ^
      "should send a 404"                       ! servletNotFoundSends404 ^
                                                bt^
    "in ScalatraFilter"                         ^
      "should invoke the chain"                 ! filterNotFoundInvokesChain^
                                                end^
  "The methodNotAllowed block"                  ^
    "should run when a route matches other methods" ! customMethodNotAllowed^
    "by default"                                ^
      "should send a 405"                       ! defaultMethodNotAllowedSends405^
      "should set the allow header"             ! allowHeader^
                                                end

  addFilter(new ScalatraFilter {}, "/filtered/*")

  addServlet(new ScalatraServlet {
    get("/fall-through") { "fell through" }
  }, "/filtered/*")

  addServlet(new ScalatraServlet {
    post("/no-get") { "foo" }
    put("/no-get") { "foo" }
  }, "/default/*")

  addServlet(new ScalatraServlet {
    post("/no-get") { "foo" }
    put("/no-get") { "foo" }

    notFound { "custom not found" }
    methodNotAllowed { _ => "custom method not allowed" }
  }, "/custom/*")

  def customNotFound = get("/custom/matches-nothing") {
    body must_== "custom not found"
  }

  def servletNotFoundSends404 = get("/default/matches-nothing") {
    status must_== 404 
  }

  def filterNotFoundInvokesChain = get("/filtered/fall-through") {
    body must_== "fell through"
  }

  def customMethodNotAllowed = get("/custom/no-get") {
    body must_== "custom method not allowed"
  }

  def defaultMethodNotAllowedSends405 = get("/default/no-get") {
    status must_== 405
  }

  def allowHeader = get("/default/no-get") {
    header("Allow").split(", ").toSet must_== Set("POST", "PUT")
  }
}

