package org.scalatra

import test.NettyBackend
import test.scalatest.ScalatraFunSuite

object DefaultRouteTest {
  val existingRoute = "/existing-route"
  val nonExistentRoute = "/no-such-route"
}

class DefaultRouteTestApp extends ScalatraApp {
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

abstract class DefaultRouteTest extends ScalatraFunSuite {
  import DefaultRouteTest._

  mount(new DefaultRouteTestApp)

  test("GET request to non-existent route should return 404") {
    get(nonExistentRoute) {
      status.code should equal (404)
    }
  }

  test("GET request to existing route should return 200") {
    get(existingRoute) {
      status.code should equal (200)
    }
  }

  test("POST request to non-existent route should return 404") {
    post(nonExistentRoute) {
      status.code should equal (404)
    }
  }

  test("POST request to existing route should return 200") {
    post(existingRoute) {
      status.code should equal (200)
    }
  }

  test("PUT request to non-existent route should return 404") {
    put(nonExistentRoute) {
      status.code should equal (404)
    }
  }

  test("PUT request to existing route should return 200") {
    put(existingRoute) {
      status.code should equal (200)
    }
  }

  test("DELETE request to non-existent route should return 404") {
    deleteReq(nonExistentRoute) {
      status.code should equal (404)
    }
  }

  test("DELETE request to existing route should return 200") {
    deleteReq(existingRoute) {
      status.code should equal (200)
    }
  }

  test("OPTIONS request to non-existent route should return 404") {
    options(nonExistentRoute) {
      status.code should equal (404)
    }
  }

  test("OPTIONS request to existing route should return 200") {
    options(existingRoute) {
      status.code should equal (200)
    }
  }
}

class NettyDefaultRouteTest extends DefaultRouteTest with NettyBackend