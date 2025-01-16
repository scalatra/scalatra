package org.scalatra

import org.scalatra.test.scalatest.ScalatraWordSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.*
import scala.concurrent.duration.*
import org.scalatest.concurrent.Eventually.*

class RouteConcurrencyServlet extends ScalatraServlet {
  private[this] val gets = for {
    i <- 0 until 250
  } yield Future { get(false) { "/" } }

  val postRoutes = for {
    i <- 0 until 250
    x = Future { post(false) { "/" } }
  } yield x

  val b = for {
    route <- postRoutes.take(250)
    x = Future { post(false) {}; post(false) {} } // add some more routes while we're removing
    y = Future { route.foreach { route => removeRoute("POST", route) } }
  } yield (x, y)
  Await.result(Future.sequence(gets ++ (b map (kv => kv._1.flatMap(_ => kv._2)))), 5.seconds)

  get("/count/:method") {
    routes(HttpMethod(params("method"))).size.toString
  }
}

class RouteConcurrencySpec extends ScalatraWordSpec {
  addServlet(classOf[RouteConcurrencyServlet], "/*")

  "A scalatra kernel " should {
    "support adding routes concurrently" in {
      get("/count/get") {
        body should equal("251") // the 500 we added in the future, plus this count route
      }
    }

    "support removing routes concurrently with adding routes" in {
      eventually {
        get("/count/post") { body should equal("500") }
      }
    }
  }
}
