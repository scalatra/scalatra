package org.scalatra.scalate

import org.scalatra.{ AsyncResult, FutureSupport, ScalatraServlet }
import org.scalatra.test.specs2.MutableScalatraSpec

import scala.concurrent.{ ExecutionContext, Future }

class ScalateI18nSupportSpec extends MutableScalatraSpec {
  addServlet(new ScalatraServlet with FutureSupport with ScalateI18nSupport {

    protected implicit def executor: ExecutionContext = ExecutionContext.global

    get("/") {
      new AsyncResult {
        val is = Future {
          mustache("hello.mustache")
        }
      }
    }

  }, "/*")

  "I18nSupport should work with Futures" in {
    get("/") {
      status should beEqualTo(200)
    }
  }

}
