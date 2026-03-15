package org.scalatra.util

import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.Inspectors

class UrlCodingUtilsSpec extends ScalatraFunSuite {

  test("containsInvalidUriChars detects invalid characters") {
    Inspectors.forEvery(
      Seq(
        "hello world",
        "hello<world",
        "hello{world",
        "hello|world",
        "hello\\world",
        "hello\"world"
      )
    ) { x =>
      withClue(s": $x") {
        UrlCodingUtils.containsInvalidUriChars(x) should be(true)
      }
    }
  }

  test("containsInvalidUriChars accepts valid characters") {
    Inspectors.forEvery(
      Seq(
        "https://example.com/api/user",
        "https://example.com/api/users/123/profile?sort=name&order=asc",
        "/api/v2/resource?foo=bar&baz=1",
        "simple",
        ""
      )
    ) { x =>
      withClue(s": $x") {
        UrlCodingUtils.containsInvalidUriChars(x) should be(false)
      }
    }
  }

  test("ensureUrlEncoding encodes if needed, skips encoding if given input doesn't meet criteria") {
    Inspectors.forEvery(
      Seq(
        "hello world"                   -> "hello%20world",
        "test<value"                    -> "test%3Cvalue",
        "hello{world"                   -> "hello%7Bworld",
        "https://example.com/path"      -> "https://example.com/path",
        "https://example.com/path-test" -> "https://example.com/path-test"
      )
    ) { case (input, expected) =>
      UrlCodingUtils.ensureUrlEncoding(input) should equal(expected)
    }
  }
}
