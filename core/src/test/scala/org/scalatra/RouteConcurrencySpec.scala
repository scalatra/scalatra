package org.scalatra

import scala.concurrent._
import ExecutionContext.Implicits.global
import test.scalatest.ScalatraWordSpec

class RouteConcurrencyServlet extends ScalatraServlet {
  for {
    i <- 0 until 250
    x = Future { get(false) { "/"} }
  } x

  val postRoutes = for {
    i <- 0 until 250
    x = Future { post(false) { "/"} }
  } yield x

  for {
    route <- postRoutes.take(250)
    x = Future { post(false) {}; post(false) {}} // add some more routes while we're removing
    y = Future { route.foreach { route => removeRoute("POST", route) }}
  } (x, y)

  get("/count/:method") {
    routes(HttpMethod(params("method"))).size.toString
  }
}

class RouteConcurrencySpec extends ScalatraWordSpec {
  addServlet(classOf[RouteConcurrencyServlet], "/*")

  "A scalatra kernel " should {
    "support adding routes concurrently" in {
      get("/count/get") {
        body should equal ("251") // the 500 we added in the future, plus this count route
      }
    }

    "support removing routes concurrently with adding routes" in {
      get("/count/post") {
        body should equal ("500")
      }
    }
  }
}
