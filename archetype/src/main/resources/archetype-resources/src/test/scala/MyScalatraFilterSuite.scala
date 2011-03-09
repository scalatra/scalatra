package net.srirangan

import org.scalatra._
import org.scalatra.test.scalatest._

import org.junit.runner.RunWith
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner]) // makes test run with Maven Surefire
class MyScalatraFilterSuite extends ScalatraFunSuite with ShouldMatchers {
  addFilter(classOf[MyScalatraFilter], "/*")

  test("GET / returns status 200") {
    get("/") { 
      status should equal (200)
    }
  }
}
