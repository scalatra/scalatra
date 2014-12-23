package org.scalatra
package servlet

import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.collection.mutable.Map

trait AttributesTest {
  this: ScalatraFunSuite =>

  trait AttributesServlet extends ScalatraServlet {
    def attributesMap: Map[String, Any]

    get("/attributes-test") {
      attributesMap("one") = "1"
      attributesMap("two") = "2"
      attributesMap("three") = "3"
      attributesMap -= "two"
      attributesMap foreach { case (k, v) => response.setHeader(k, v.toString) }
    }
  }

  test("apply should set request attribute") {
    get("/attributes-test") {
      header("one") should equal("1")
      header("three") should equal("3")
    }
  }

  test("-= should remove request attribute") {
    get("/attributes-test") {
      header("two") should equal(null)
    }
  }
}
