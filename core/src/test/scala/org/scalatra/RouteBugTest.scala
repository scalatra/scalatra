package org.scalatra

import org.scalatest.FunSuite
import org.scalatra.test.scalatest.ScalatraSuite

class RouteBugTest extends FunSuite with ScalatraSuite {

  val servlet = new ScalatraServlet {

    get("/captures/(.*)".r) {
      multiParams("captures").head
    }

    get("/sinatra/:thing") {
      params("thing")
    }

    get("/:thing/length") {
      params("thing").length.toString
    }

  }

  addServlet(servlet, "/*")

  test("captures") {
    get("/captures/ok%3Bgo") {
      body should be("ok;go")
    }
  }

  test("sinatra-like params") {
    get("/sinatra/wink%3B)") {
      body should be("wink;)")
    }
  }

  test("matches route with semi colon in parametrised segment") {
    get("/semi%3Bcolon/length") {
      body should be("10")
    }
  }
}