package org.scalatra

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraSuite
import scala.concurrent.ops._

class RouteConcurrencyServlet extends ScalatraServlet {
  for {
    i <- 0 until 500
    x = future { get(false) { "/"} }
  } x()

  get("/count") {
    Routes("GET").size
  }
}

class RouteConcurrencySpec extends WordSpec with ScalatraSuite with ShouldMatchers {
  addServlet(classOf[RouteConcurrencyServlet], "/*")

  "A scalatra kernel " should {
    "support adding routes concurrently" in {
      get("/count") {
        body should equal ("501")
      }
    }
  }
}