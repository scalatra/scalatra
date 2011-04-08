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

  val postRoutes = for {
    i <- 0 until 500
    x = future { post(false) { "/"} }
  } yield x()

  for {
    route <- postRoutes.take(250)
    x = future { post(false) {}; post(false) {}} // add some more routes while we're removing
    y = future { removeRoute("POST", route) }
  } (x(), y())

  get("/count/:method") {
    routes(HttpMethod(params("method"))).size
  }
}

class RouteConcurrencySpec extends WordSpec with ScalatraSuite with ShouldMatchers {
  addServlet(classOf[RouteConcurrencyServlet], "/*")

  "A scalatra kernel " should {
    "support adding routes concurrently" in {
      get("/count/get") {
        body should equal ("501") // the 500 we added in the future, plus this count route
      }
    }

    "support removing routes concurrently with adding routes" in {
      get("/count/post") {
        body should equal ("750")
      }
    }
  }
}