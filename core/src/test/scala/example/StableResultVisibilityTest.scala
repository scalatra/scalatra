package example

// Don't import org.scalatra._ here to detect macros errors
import org.scalatra.ScalatraServlet
import org.scalatra.test.scalatest.ScalatraFunSuite

/*
> scalatra/test

[error] /xxx/scalatra/core/src/test/scala/example/StableResultVisibilityTest.scala:9: class StableResult in package scalatra cannot be accessed in package org.scalatra
[error]  Access to protected class StableResult not permitted because
[error]  enclosing object SimpleServlet in class StableResultVisibilityTest is not a subclass of
[error]  package scalatra in package org where target is defined
[error]     get("/") {
[error]              ^
[error] one error found
[error] (scalatra/test:compileIncremental) Compilation failed
 */
class StableResultVisibilityTest extends ScalatraFunSuite {

  class SimpleServlet extends ScalatraServlet {

    // #525 [2.4.0.RC2-1] "not found: value Route"
    before() {
    }
    before("/") {
    }

    get("/") {
      "ok"
    }
    trap(100) {
    }

    after() {
    }
    after("/") {
    }

    case class FooException(msg: String) extends Exception

    error {
      case scala.util.control.NonFatal(e) =>
    }

    notFound {
    }

    methodNotAllowed {
      case methods =>
    }
  }
  addServlet(new SimpleServlet, "/*")

  test("Accessing StableResult via macros should be allowed") {
    get("/") {
      status should equal(200)
      body should equal("ok")
    }
  }
}