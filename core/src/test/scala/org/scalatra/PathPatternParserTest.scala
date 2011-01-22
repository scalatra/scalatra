package org.scalatra

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class PathPatternParserTest extends FunSuite with ShouldMatchers {
  test("should match exactly on a simple path") {
    val PathPattern(pattern, names) = PathPatternParser.parseFrom("/simple/path")

    pattern.toString should equal ("""^/\Qsimple\E/\Qpath\E$""")
    names should equal (Nil)
  }

  test("should match with a trailing slash") {
    val PathPattern(pattern, names) = PathPatternParser.parseFrom("/simple/path/")

    pattern.toString should equal ("""^/\Qsimple\E/\Qpath\E/$""")
    names should equal (Nil)
  }
  
  test("should replace a splat with a capturing group") {
    val PathPattern(pattern, names) = PathPatternParser.parseFrom("/splat/path/*")

    pattern.toString should equal ("""^/\Qsplat\E/\Qpath\E/(.*?)$""")
    names should equal (List("splat"))
  }

  test("should capture named groups") {
    val PathPattern(pattern, names) = PathPatternParser.parseFrom("/path/:group")

    pattern.toString should equal ("""^/\Qpath\E/([^/?]+)$""")
    names should equal (List("group"))
  }

  test("should escape special regex characters") {
    val PathPattern(pattern, names) = PathPatternParser.parseFrom("/special/$.+()")

    pattern.toString should equal ("""^/\Qspecial\E/\Q$.+()\E$""")
    names should equal (Nil)
  }

  test("allow optional named groups") {
    val PathPattern(pattern, names) = PathPatternParser.parseFrom("/optional/?:stuff?")

    pattern.toString should equal ("""^/\Qoptional\E/?([^/?]+)?$""")
    names should equal (List("stuff"))
  }

  test("should support seperate named params for filename and extension") {
    val PathPattern(pattern, names) = PathPatternParser.parseFrom("/path-with/:file.:extension")

    pattern.toString should equal ("""^/\Qpath-with\E/([^/?]+)\.([^/?]+)$""")
    names should equal (List("file", "extension"))
  }
}