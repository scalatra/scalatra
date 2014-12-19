package org.scalatra

import org.scalatest.FunSuite
import org.scalatest.Matchers

/**
 * Test cases adapted from  https://github.com/josh/rack-mount/blob/d44e02ec8a6318fdda8ea53a30aac654e228e07b/test/test_strexp.rb
 */
class RailsPathPatternParserTest extends FunSuite with Matchers {

  test("static string") {
    val PathPattern(re, names) = RailsPathPatternParser("foo")
    re.toString should equal("""\Afoo\Z""")
    names should equal(Nil)
  }

  test("dynamic segment") {
    val PathPattern(re, names) = RailsPathPatternParser(":foo.example.com")
    re.toString should equal("""\A([^#/.?]+)\.example\.com\Z""")
    names should equal(List("foo"))
  }

  test("dynamic segment with leading underscore") {
    val PathPattern(re, names) = RailsPathPatternParser(":_foo.example.com")
    re.toString should equal("""\A([^#/.?]+)\.example\.com\Z""")
    names should equal(List("_foo"))
  }

  test("skips invalid group names: 123") {
    val PathPattern(re, names) = RailsPathPatternParser(":123.example.com")
    re.toString should equal("""\A:123\.example\.com\Z""")
    names should equal(Nil)
  }

  test("skips invalid group names: $") {
    val PathPattern(re, names) = RailsPathPatternParser(":$.example.com")
    re.toString should equal("""\A:\$\.example\.com\Z""")
    names should equal(Nil)
  }

  test("escaped dynamic segment") {
    val PathPattern(re, names) = RailsPathPatternParser("\\:foo.example.com")
    re.toString should equal("""\A:foo\.example\.com\Z""")
    names should equal(Nil)
  }

  test("dynamic segment inside optional segment") {
    val PathPattern(re, names) = RailsPathPatternParser("foo(.:extension)")
    re.toString should equal("""\Afoo(?:\.([^#/.?]+))?\Z""")
    names should equal(List("extension"))
  }

  test("glob segment") {
    val PathPattern(re, names) = RailsPathPatternParser("src/*files")
    re.toString should equal("""\Asrc/(.+)\Z""")
    names should equal(List("files"))
  }

  test("glob segment at the beginning") {
    val PathPattern(re, names) = RailsPathPatternParser("*files/foo.txt")
    re.toString should equal("""\A(.+)/foo\.txt\Z""")
    names should equal(List("files"))
  }

  test("glob segment in the middle") {
    val PathPattern(re, names) = RailsPathPatternParser("src/*files/foo.txt")
    re.toString should equal("""\Asrc/(.+)/foo\.txt\Z""")
    names should equal(List("files"))
  }

  test("multiple glob segments") {
    val PathPattern(re, names) = RailsPathPatternParser("src/*files/dir/*morefiles/foo.txt")
    re.toString should equal("""\Asrc/(.+)/dir/(.+)/foo\.txt\Z""")
    names should equal(List("files", "morefiles"))
  }

  test("escaped glob segment") {
    val PathPattern(re, names) = RailsPathPatternParser("src/\\*files")
    re.toString should equal("""\Asrc/\*files\Z""")
    names should equal(Nil)
  }

  test("optional segment") {
    val PathPattern(re, names) = RailsPathPatternParser("/foo(/bar)")
    re.toString should equal("""\A/foo(?:/bar)?\Z""")
    names should equal(Nil)
  }

  test("consecutive optional segment") {
    val PathPattern(re, names) = RailsPathPatternParser("/foo(/bar)(/baz)")
    re.toString should equal("""\A/foo(?:/bar)?(?:/baz)?\Z""")
    names should equal(Nil)
  }

  test("multiple optional segments") {
    val PathPattern(re, names) = RailsPathPatternParser("(/foo)(/bar)(/baz)")
    re.toString should equal("""\A(?:/foo)?(?:/bar)?(?:/baz)?\Z""")
    names should equal(Nil)
  }

  test("escapes optional segment parentheses") {
    val PathPattern(re, names) = RailsPathPatternParser("""/foo\(/bar\)""")
    re.toString should equal("""\A/foo\(/bar\)\Z""")
    names should equal(Nil)
  }

  test("escapes one optional segment parenthesis") {
    val PathPattern(re, names) = RailsPathPatternParser("""/foo\((/bar)""")
    re.toString should equal("""\A/foo\((?:/bar)?\Z""")
    names should equal(Nil)
  }
}
