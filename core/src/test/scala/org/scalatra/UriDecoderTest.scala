package org.scalatra

import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.Inspectors

class UriDecoderTest extends ScalatraFunSuite {

  test("Double encoding is decoded only once") {
    // %2520 is double-encoded space (%20 -> %2520)
    // Should decode to %20, not to a space
    UriDecoder.decode("test%2520value") should equal("test%20value")
  }

  test("Non-encoded values can be decoded") {
    val expected = "https://example.com/foo/bar?test=80&test ab~~"
    UriDecoder.decode(expected) should equal(expected)
  }

  test("valid URIs pass through unchanged") {
    Inspectors.forEvery(
      Seq(
        "https://example.com/path/to/resource",
        "https://example.com/path#section",
        "https://example.com/path?key=value&another=test",
        "https://example.com:8080/path?query=value",
        "https://user:pass@example.com/path",
      )
    ) { uri =>
      withClue(s": $uri") {
        UriDecoder.decode(uri) should equal(uri)
      }
    }
  }

  test("encoded characters decode correctly") {
    Inspectors.forEvery(
      Seq(
        "https%3A%2F%2Fexample.com%2Ffoo"                 -> "https://example.com/foo",
        "test%20%20%20%20multiple"                        -> "test    multiple",
        "test%21%40%23%24%25"                             -> "test!@#$%",
        "test%2Fvalue"                                    -> "test/value",
        "test%2fvalue"                                    -> "test/value", // lowercase
        "https://example.com/foo%20bar/baz"               -> "https://example.com/foo bar/baz",
        "https://example.com/%E3%81%93%E3%82%93%E3%81%AB" -> "https://example.com/こんに",
        "test%20%F0%9F%98%80%20emoji"                     -> "test 😀 emoji",
        "https://example.com?q=a%26b"                     -> "https://example.com?q=a&b",
        "https://example.com?key=value%3Danother"         -> "https://example.com?key=value=another",
        "https://example.com/path/with%3Acolon"           -> "https://example.com/path/with:colon",
        "https%3A%2F%2Fexample.com%2Ffoo%2Fbar%3Ftest%3D80%26foo%3Dtest%20ab" -> "https://example.com/foo/bar?test=80&foo=test ab"
      )
    ) { case (input, expected) =>
      withClue(s": $input") {
        UriDecoder.decode(input) should equal(expected)
      }
    }
  }

  test("Specific cases") {
    Inspectors.forEvery(
      Seq(
        "test%"        -> "test%",   // single % at end
        "test%ZZ"      -> "test%ZZ", // invalid hex
        "test%2"       -> "test%2",  // incomplete
        "%"            -> "%",       // only percent sign
        ""             -> "",
        "test+value"   -> "test+value",
        "test%2Bvalue" -> "test+value",
        "test%00value" -> "test\u0000value",
        "test%0Avalue" -> "test\nvalue"
      )
    ) { case (input, expected) =>
      withClue(s": $input") {
        UriDecoder.decode(input) should equal(expected)
      }
    }
  }
}
