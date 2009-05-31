package com.thinkminimo.step
import org.scalatest._

class StepSuite extends FunSuite with PrivateMethodTester {
  test("true should equal true") {
    val t = true
    assert(t == true)
  }
}
