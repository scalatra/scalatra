package org.scalatra

import test.scalatest.ScalatraFunSuite

object DefaultRouteTest {
  val existingRoute = "/existing-route"
  val nonExistentRoute = "/no-such-route"
}

class DefaultRouteTestServlet extends ScalatraServlet {
  import DefaultRouteTest._

  get(existingRoute) {
    "get"
  }

  post(existingRoute) {
    "post"
  }

  put(existingRoute) {
    "put"
  }

  delete(existingRoute) {
    "delete"
  }

  options(existingRoute) {
    "options"
  }
}

class DefaultRouteTest extends ScalatraFunSuite {
  import DefaultRouteTest._

  addServlet(classOf[DefaultRouteTestServlet], "/*")

  test("GET request to non-existent route should return 404") {
    get(nonExistentRoute) {
      status should equal(404)
    }
  }

  test("GET request to existing route should return 200") {
    get(existingRoute) {
      status should equal(200)
    }
  }

  test("POST request to non-existent route should return 404") {
    post(nonExistentRoute) {
      status should equal(404)
    }
  }

  test("POST request to existing route should return 200") {
    post(existingRoute) {
      status should equal(200)
    }
  }

  test("PUT request to non-existent route should return 404") {
    put(nonExistentRoute) {
      status should equal(404)
    }
  }

  test("PUT request to existing route should return 200") {
    put(existingRoute) {
      status should equal(200)
    }
  }

  test("DELETE request to non-existent route should return 404") {
    delete(nonExistentRoute) {
      status should equal(404)
    }
  }

  test("DELETE request to existing route should return 200") {
    delete(existingRoute) {
      status should equal(200)
    }
  }

  test("OPTIONS request to non-existent route should return 404") {
    options(nonExistentRoute) {
      status should equal(404)
    }
  }

  test("OPTIONS request to existing route should return 200") {
    options(existingRoute) {
      status should equal(200)
    }
  }
}
