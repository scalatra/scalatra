package org.scalatra.util

import org.specs2.Specification

class NotEmptyTest extends Specification { def is =

  "NotEmpty should" ^
    "extract None from null" ! {
      NotEmpty.unapply(null:String) must beNone
    } ^
    "extracts None from an empty string" ! {
      NotEmpty.unapply("") must beNone
    } ^
    "extracts the string from a blank string" ! {
      NotEmpty.unapply("  ") should beSome("  ")
    } ^
    "extracts the string from a non-blank string" ! {
      NotEmpty.unapply("foo") should beSome("foo")
    } ^
    "extracts None from None" ! {
      NotEmpty.unapply(None) should beNone
    } ^
    "extracts None from Some(null)" ! {
      NotEmpty.unapply(Some(null)) should beNone
    } ^
    "extracts None from Some(emptyString)" ! {
      NotEmpty.unapply(Some("")) should beNone
    } ^
    "extracts Some(blankString) from Some(blankString)" ! {
      NotEmpty.unapply(Some("  ")) should beSome("  ")
    } ^
    "extracts Some(nonBlankString) from Some(nonBlankString)" ! {
      NotEmpty.unapply(Some("foo")) should beSome("foo")
    } ^ end
}
