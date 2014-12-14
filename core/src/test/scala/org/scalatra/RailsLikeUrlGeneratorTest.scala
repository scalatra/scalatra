package org.scalatra

import org.scalatest.{ Matchers, FunSuite }
import org.scalatest.matchers.MustMatchers

class RailsLikeUrlGeneratorTest extends FunSuite with Matchers {

  def url(path: String, params: Tuple2[String, String]*): String =
    url(path, params.toMap)

  def url(path: String, splat: String, moreSplats: String*): String =
    url(path, Map[String, String](), splat +: moreSplats)

  def url(path: String, params: Map[String, String] = Map(), splats: Iterable[String] = Seq()): String =
    new RailsRouteMatcher(path).reverse(params, splats.toList)

  test("static string") {
    url("/foo") should equal("/foo")
  }

  test("dynamic segment") {
    url(":foo.example.com", "foo" -> "vanilla") should equal("vanilla.example.com")
  }

  test("dynamic segment with leading underscore") {
    url(":_foo.example.com", "_foo" -> "vanilla") should equal("vanilla.example.com")
  }

  test("skip invalid group names: 123") {
    url(":123.example.com") should equal(":123.example.com")
  }

  test("skip invalid group names: $") {
    url(":$.example.com") should equal(":$.example.com")
  }

  test("escaped dynamic segment") {
    url("""\:foo.example.com""") should equal(":foo.example.com")
    url("""bar.\:foo.com""") should equal("bar.:foo.com")
  }

  test("dynamic segment inside optional segment") {
    url("foo(.:extension)", "extension" -> "json") should equal("foo.json")
    url("foo(.:extension)") should equal("foo")
  }

  test("static string and dynamic segment inside optional segment") {
    url("foo(/bar.:extension)", "extension" -> "json") should equal("foo/bar.json")
    url("foo(/bar.:extension)") should equal("foo")
  }

  test("glob segment") {
    url("src/*files", "files" -> "a/b/c.txt") should equal("src/a/b/c.txt")
  }

  test("glob segment at the beginning") {
    url("*files/foo.txt", "files" -> "/home/thib") should equal("/home/thib/foo.txt")
  }

  test("glob segment in the middle") {
    url("src/*files/foo.txt", "files" -> "a/b/c") should equal("src/a/b/c/foo.txt")
  }

  test("multiple glob segments") {
    url("src/*files/dir/*morefiles/foo.txt", "files" -> "a/b", "morefiles" -> "c/d") should equal("src/a/b/dir/c/d/foo.txt")
  }

  test("escaped glob segment") {
    url("""src/\*files""") should equal("src/*files")
  }

  test("glob segment inside optional segment") {
    url("src(/*files)", "files" -> "a/b/c.txt") should equal("src/a/b/c.txt")
    url("src(/*files)") should equal("src")
  }

  test("optional segment") {
    url("/foo(/bar)") should equal("/foo/bar")
  }

  test("optional segment on first position") {
    url("(/foo)/bar") should equal("/foo/bar")
  }

  test("consecutive optional segments") {
    url("/foo(/bar)(/baz)") should equal("/foo/bar/baz")
  }

  test("separated optional segments") {
    url("/foo(/bar)/buz(/baz)") should equal("/foo/bar/buz/baz")
  }

  test("multiple optional segments") {
    url("(/foo)(/bar)(/baz)") should equal("/foo/bar/baz")
  }

  test("escapes optional segment parentheses") {
    url("""/foo\(/bar\)""") should equal("/foo(/bar)")
  }

  test("escapes one optional segment parenthesis") {
    url("""/foo\((/bar)""") should equal("/foo(/bar")
  }
}
