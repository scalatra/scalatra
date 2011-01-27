package org.scalatra

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class FlashMapTest extends FunSuite with ShouldMatchers with BeforeAndAfterEach {
  var flash: FlashMap = _

  override def beforeEach = flash = FlashMap()

  test("items are not visible until the first sweep") {
    flash("foo") = "bar"
    flash.get("foo") should equal (None)
  }

  test("items are visible after the first sweep") {
    flash("foo") = "bar"
    flash.sweep()
    flash.get("foo") should equal (Some("bar"))
  }

  test("items are not visible after the second sweep") {
    flash("foo") = "bar"
    flash.sweep()
    flash.sweep()
    flash.get("foo") should equal (None)
  }

  test("items are not overwritten in the current map") {
    flash("foo") = "bar"
    flash.sweep()
    flash("foo") = "baz"
    flash.get("foo") should equal (Some("bar"))
  }

  test("items are removed from the next map") {
    flash("foo") = "bar"
    flash -= "foo"
    flash.sweep()
    flash.get("foo") should equal (None)
  }

  test("items are not removed from the current map") {
    flash("foo") = "bar"
    flash.sweep()
    flash -= "foo"
    flash.get("foo") should equal (Some("bar"))
  }

  test("iterates over only the current map") {
    flash("one") = 1
    flash("two") = 2
    flash.sweep()
    flash("three") = 3
    flash.iterator.toSeq should equal (Seq("one" -> 1, "two" -> 2))
  }

  test("keep adds all items from the current map to the next map") {
    flash("1") = "one"
    flash("2") = "two"
    flash.sweep()
    flash("1") = "uno"
    flash("3") = "tres"
    flash.keep()
    flash.sweep()
    flash should equal (Map("1" -> "one", "2" -> "two", "3" -> "tres"))
  }

  test("keep with an argument retains only that key") {
    flash("1") = "one"
    flash("2") = "two"
    flash.sweep()
    flash("1") = "uno"
    flash("3") = "tres"
    flash.keep("1")
    flash.sweep()
    flash should equal (Map("1" -> "one", "3" -> "tres"))
  }

  test("exposes current map for mutation throgh 'now'") {
    flash("foo") = "bar"
    flash.now("foo") = "baz"
    flash.get("foo") should equal (Some("baz"))
  }

  test("is accessible via symbols") {
    val fsh = FlashMap()
    fsh("foo") = "bar"
    fsh.sweep
    fsh("foo") should equal ("bar")
    fsh('foo) should equal ("bar")
  }

  test("is serializable") {
    val out = new ObjectOutputStream(new ByteArrayOutputStream)
    out.writeObject(new FlashMap)
  }
}
