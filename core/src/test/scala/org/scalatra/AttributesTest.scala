package org.scalatra

import scala.collection.mutable.Map
import org.scalatest.matchers.ShouldMatchers
import test.scalatest.ScalatraFunSuite

trait AttributesTest {
  this: ScalatraFunSuite with ShouldMatchers =>

  trait AttributesServlet extends ScalatraServlet {
    def attributesMap: Map[String, AnyRef]

    get("/attributes-test") {
      attributesMap("one") = "1"
      attributesMap("two") = "2"
      attributesMap("three") = "3"
      attributesMap -= "two"
      attributesMap foreach { case(k, v) => response.setHeader(k, v.toString) }
    }
  }

  test("apply should set request attribute") {
    get("/attributes-test") {
      header("one") should equal ("1")
      header("three") should equal ("3")
    }
  }

  test("-= should remove request attribute") {
    get("/attributes-test") {
      header("two") should equal (null)
    }
  }
}
