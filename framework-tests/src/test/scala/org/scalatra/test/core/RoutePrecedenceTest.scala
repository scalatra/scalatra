package org.scalatra

import test.NettyBackend
import test.JettyBackend
import test.scalatest.ScalatraFunSuite

class RoutePrecedenceTestBaseServlet extends ScalatraApp {
  get("/override-route") {
    "base"
  }
}

class RoutePrecedenceTestChildServlet extends RoutePrecedenceTestBaseServlet {
  get("/override-route") {
    "child"
  }

  get("/hide-route") {
    "hidden by later route"
  }

  get("/hide-route") {
    "visible"
  }

  get("/pass") {
    write("3")
  }

  get("/pass") {
    write("1")
    pass()
    write("2")
  }

  get("/pass-to-not-found") {
    write("a")
    pass()
    write("b")
  }

  get("/do-not-pass") {
    write("2")
  }

  get("/do-not-pass") {
    write("1")
  }

  notFound {
    write("c")
  }

  def write(value: String) = {
    response.outputStream.write(value.getBytes(response.characterEncoding getOrElse "UTF-8"))
  }

}

abstract class RoutePrecedenceTest extends ScalatraFunSuite {
  mount(new RoutePrecedenceTestChildServlet)

  test("Routes in child should override routes in base") {
    get("/override-route") {
      body should equal ("child")
    }
  }

  test("Routes declared later in the same class take precedence") {
    /*
     * This is the opposite of Sinatra, where the earlier route wins.  But to do otherwise, while also letting child
     * classes override base classes' routes, proves to be difficult in an internal Scala DSL.  Sorry, Sinatra users.
     */
    get("/hide-route") {
      "visible"
    }
  }

  test("pass immediately passes to next matching route") {
    get("/pass") {
      body should equal ("13")
    }
  }

  test("pass invokes notFound action if no more matching routes") {
    get("/pass-to-not-found") {
      body should equal ("ac")
    }
  }

  test("does not keep executing routes without pass") {
    get("/do-not-pass") {
      body should equal ("1")
    }
  }
}

class NettyRoutePrecedenceTest extends RoutePrecedenceTest with NettyBackend
class JettyRoutePrecedenceTest extends RoutePrecedenceTest with JettyBackend
