package org.scalatra

import org.scalatra.test.scalatest.ScalatraFunSuite

class UriDecoderTest extends ScalatraFunSuite {

  test("Non-encoded values can be decoded") {
    val expected = "https://example.com/foo/bar?test=80&test ab~~"
    UriDecoder.decode(expected) should equal(expected)
  }

  test("Encoded values can be decoded") {
    val expected = "https://example.com/foo/bar?test=80&foo=test ab"
    UriDecoder.decode("https%3A%2F%2Fexample.com%2Ffoo%2Fbar%3Ftest%3D80%26foo%3Dtest%20ab") should equal(expected)
  }

  test("Already decoded values remain unchanged") {
    val uri = "https://example.com/path/to/resource"
    UriDecoder.decode(uri) should equal(uri)
  }

  test("Partially encoded URIs are decoded") {
    val expected = "https://example.com/foo bar/baz"
    UriDecoder.decode("https://example.com/foo%20bar/baz") should equal(expected)
  }

  test("Special characters are decoded correctly") {
    UriDecoder.decode("test%21%40%23%24%25") should equal("test!@#$%")
  }

  test("Plus signs are NOT treated as spaces (plusIsSpace = false)") {
    UriDecoder.decode("test+value") should equal("test+value")
    UriDecoder.decode("test%2Bvalue") should equal("test+value")
  }

  test("Unicode characters are decoded correctly") {
    val expected = "https://example.com/こんにちは"
    UriDecoder.decode("https://example.com/%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF") should equal(expected)
  }

  test("Emoji are decoded correctly") {
    val expected = "test 😀 emoji"
    UriDecoder.decode("test%20%F0%9F%98%80%20emoji") should equal(expected)
  }

  test("Mixed encoded and non-encoded characters") {
    val expected = "https://example.com/path?query=value with spaces&other=test"
    UriDecoder.decode("https://example.com/path?query=value%20with%20spaces&other=test") should equal(expected)
  }

  test("Reserved characters in URI components") {
    val expected = "https://example.com/path?key=value&another=test"
    UriDecoder.decode("https://example.com/path?key=value&another=test") should equal(expected)
  }

  test("Handles malformed percent encoding gracefully") {
    // Single % at end
    UriDecoder.decode("test%") should equal("test%")

    // Invalid hex characters
    UriDecoder.decode("test%ZZ") should equal("test%ZZ")

    // Incomplete encoding
    UriDecoder.decode("test%2") should equal("test%2")
  }

  test("Empty string") {
    UriDecoder.decode("") should equal("")
  }

  test("Only percent sign") {
    UriDecoder.decode("%") should equal("%")
  }

  test("Multiple consecutive spaces encoded") {
    val expected = "test    multiple"
    UriDecoder.decode("test%20%20%20%20multiple") should equal(expected)
  }

  test("Fragment identifiers are preserved") {
    val expected = "https://example.com/path#section"
    UriDecoder.decode("https://example.com/path#section") should equal(expected)
  }

  test("Uppercase and lowercase percent encoding") {
    UriDecoder.decode("test%2Fvalue") should equal("test/value")
    UriDecoder.decode("test%2fvalue") should equal("test/value")
  }

  test("Encoded slashes and colons in path") {
    val expected = "https://example.com/path/with:colon"
    UriDecoder.decode("https://example.com/path/with%3Acolon") should equal(expected)
  }

  test("Query string with encoded ampersands") {
    val expected = "https://example.com?q=a&b"
    UriDecoder.decode("https://example.com?q=a%26b") should equal(expected)
  }

  test("Encoded equals signs in query values") {
    val expected = "https://example.com?key=value=another"
    UriDecoder.decode("https://example.com?key=value%3Danother") should equal(expected)
  }

  test("URI with port number") {
    val expected = "https://example.com:8080/path?query=value"
    UriDecoder.decode("https://example.com:8080/path?query=value") should equal(expected)
  }

  test("Double encoding is decoded only once") {
    // %2520 is double-encoded space (%20 -> %2520)
    // Should decode to %20, not to a space
    UriDecoder.decode("test%2520value") should equal("test%20value")
  }

  test("Null bytes and control characters") {
    UriDecoder.decode("test%00value") should equal("test\u0000value")
    UriDecoder.decode("test%0Avalue") should equal("test\nvalue")
  }

  test("URI with authentication") {
    val expected = "https://user:pass@example.com/path"
    UriDecoder.decode("https://user:pass@example.com/path") should equal(expected)
  }

  test("Very long URI") {
    val path = "a" * 1000
    val uri  = s"https://example.com/$path"
    UriDecoder.decode(uri) should equal(uri)
  }

  import org.scalatra.util.UrlCodingUtils

  test("containsInvalidUriChars detects invalid characters") {
    // Direct tests of the detection logic
    UrlCodingUtils.containsInvalidUriChars("hello world") should be(true)  // space is invalid
    UrlCodingUtils.containsInvalidUriChars("hello<world") should be(true)  // < is invalid
    UrlCodingUtils.containsInvalidUriChars("hello{world") should be(true)  // { is invalid
    UrlCodingUtils.containsInvalidUriChars("hello|world") should be(true)  // | is invalid
    UrlCodingUtils.containsInvalidUriChars("hello\\world") should be(true) // \ is invalid
    UrlCodingUtils.containsInvalidUriChars("hello\"world") should be(true) // " is invalid
  }

  test("containsInvalidUriChars accepts valid URI characters") {
    UrlCodingUtils.containsInvalidUriChars("https://example.com/path?query=value") should be(false)
    UrlCodingUtils.containsInvalidUriChars("hello-world_test.txt") should be(false)
    UrlCodingUtils.containsInvalidUriChars("ABCabc123") should be(false)
    UrlCodingUtils.containsInvalidUriChars("user@example.com") should be(false)
    UrlCodingUtils.containsInvalidUriChars("test!$&'()*+,;=") should be(false)
  }

  test("needsUrlEncoding detects unencoded invalid characters") {
    UrlCodingUtils.needsUrlEncoding("hello world") should be(true)
    UrlCodingUtils.needsUrlEncoding("hello<world") should be(true)
  }

  test("needsUrlEncoding returns false for already encoded strings") {
    UrlCodingUtils.needsUrlEncoding("hello%20world") should be(false)
    UrlCodingUtils.needsUrlEncoding("test%3Cvalue") should be(false)
  }

  test("needsUrlEncoding returns false for valid unencoded URIs") {
    UrlCodingUtils.needsUrlEncoding("https://example.com/path") should be(false)
  }

  test("ensureUrlEncoding encodes unencoded invalid characters") {
    UrlCodingUtils.ensureUrlEncoding("hello world") should equal("hello%20world")
    UrlCodingUtils.ensureUrlEncoding("test<value") should include("%3C")
  }

  test("ensureUrlEncoding leaves already encoded strings unchanged") {
    val encoded = "hello%20world"
    UrlCodingUtils.ensureUrlEncoding(encoded) should equal(encoded)
  }

  test("ensureUrlEncoding leaves valid unencoded URIs unchanged") {
    val uri = "https://example.com/path"
    UrlCodingUtils.ensureUrlEncoding(uri) should equal(uri)
  }
}
