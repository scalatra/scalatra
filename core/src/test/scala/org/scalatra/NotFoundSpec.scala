package org.scalatra

import test.specs2.ScalatraSpec

class NotFoundSpec extends ScalatraSpec {
  def is = s2"""
  The notFound block
    should run when no route matches $customNotFound
    should not result in a 404 if the block doesn't $customNotFoundStatus
    in ScalatraServlet
      should send a 404 $servletNotFoundSends404
    in ScalatraFilter
      should invoke the chain $filterNotFoundInvokesChain

  The methodNotAllowed block
    should run when a route matches other methods $customMethodNotAllowed
    should support pass $passFromNotAllowed
    by default
      should send a 405 $defaultMethodNotAllowedSends405
      should set the allow header $allowHeader
    HEAD should be implied by GET
      should pass in a filter by default $methodNotAllowedFilterPass
  """

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

  def customNotFound = get("/custom/matches-nothing") {
    body must_== "custom not found"
  }

  def customNotFoundStatus = get("/custom/matches-nothing") {
    status must_== 200
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

  def getImpliesHead = post("/default/get") {
    header("Allow").split(", ").toSet must_== Set("GET", "HEAD")
  }

  def passFromNotAllowed = get("/pass-from-not-allowed/no-get") {
    body must_== "fell through"
  }

  def methodNotAllowedFilterPass = get("/filtered/get") {
    body must_== "servlet get"
  }
}
