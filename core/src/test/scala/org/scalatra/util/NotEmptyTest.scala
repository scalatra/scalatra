package org.scalatra.util

import org.scalatest.{ Matchers, FunSuite }
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class NotEmptyTest extends FunSuite with Matchers {
  test("extracts None from null") {
    NotEmpty.unapply(null: String) should equal(None)
  }

  test("extracts None from an empty string") {
    NotEmpty.unapply("") should equal(None)
  }

  test("extracts the string from a blank string") {
    NotEmpty.unapply("  ") should equal(Some("  "))
  }

  test("extracts the string from a non-blank string") {
    NotEmpty.unapply("foo") should equal(Some("foo"))
  }

  test("extracts None from None") {
    NotEmpty.unapply(None) should equal(None)
  }

  test("extracts None from Some(null)") {
    NotEmpty.unapply(Some(null)) should equal(None)
  }

  test("extracts None from Some(emptyString)") {
    NotEmpty.unapply(Some("")) should equal(None)
  }

  test("extracts Some(blankString) from Some(blankString)") {
    NotEmpty.unapply(Some("  ")) should equal(Some("  "))
  }

  test("extracts Some(nonBlankString) from Some(nonBlankString)") {
    NotEmpty.unapply(Some("foo")) should equal(Some("foo"))
  }
}
