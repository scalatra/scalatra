package org.scalatra

import java.io.{ ByteArrayOutputStream, ObjectOutputStream }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ BeforeAndAfterEach, FunSuite, Matchers }

@RunWith(classOf[JUnitRunner])
class FlashMapTest extends FunSuite with Matchers with BeforeAndAfterEach {
  var flash: FlashMap = _

  override def beforeEach = flash = new FlashMap()

  test("values are visible immmediately") {
    flash("foo") = "bar"
    flash.get("foo") should equal(Some("bar"))
  }

  test("gotten values are removed on sweep") {
    flash("foo") = "bar"
    flash.get("foo")
    flash.get("foo") should equal(Some("bar"))
    flash.sweep()
    flash.get("foo") should equal(None)
  }

  test("ungotten values are not removed on sweep") {
    flash("foo") = "bar"
    flash.sweep()
    flash.get("foo") should equal(Some("bar"))
  }

  test("values are overwritten immediately") {
    flash("foo") = "bar"
    flash.sweep()
    flash.get("foo") should equal(Some("bar"))
    flash("foo") = "baz"
    flash.get("foo") should equal(Some("baz"))
  }

  test("values overwritten since last gotten are not removed on sweep") {
    flash("foo") = "bar"
    flash.get("foo")
    flash("foo") = "baz"
    flash.sweep()
    flash.get("foo") should equal(Some("baz"))
  }

  test("gotten keys are not remembered across sweeps") {
    flash("foo") = "bar"
    flash.get("foo")
    flash.sweep()
    flash("foo") = "baz"
    flash.sweep()
    flash.get("foo") should equal(Some("baz"))
  }

  test("values are removed immediately") {
    flash("foo") = "bar"
    flash -= "foo"
    flash.get("foo") should equal(None)
  }

  test("iterates over previously and currently added keys") {
    flash("one") = 1
    flash("two") = 2
    flash.sweep()
    flash("three") = 3
    flash.toSet should equal(Set("one" -> 1, "two" -> 2, "three" -> 3))
  }

  test("iterated keys are removed on sweep") {
    val keys = Set("1", "2")
    keys foreach { k => flash(k) = true }
    // Iteration order is unspecified
    val (gottenKey, _) = flash.iterator.next
    val ungottenKey = (keys - gottenKey).head
    flash.sweep()
    flash.get(gottenKey) should equal(None)
    flash.get(ungottenKey) should equal(Some(true))
  }

  test("keep without arguments retains used keys through one sweep") {
    flash("1") = "one"
    flash("2") = "two"
    flash.get("1")
    flash.keep()
    flash.sweep()
    flash.get("1") should equal(Some("one"))
    flash.get("2") should equal(Some("two"))
    flash.sweep()
    flash.get("1") should equal(None)
  }

  test("keep with an argument retains just those keys through one sweep") {
    flash("1") = "one"
    flash("2") = "two"
    flash("3") = "three"
    flash.get("1")
    flash.get("2")
    flash.get("3")
    flash.keep("1")
    flash.keep("3")
    flash.sweep()
    flash.get("1") should equal(Some("one"))
    flash.get("2") should equal(None)
    flash.get("3") should equal(Some("three"))
    flash.sweep()
    flash.get("1") should equal(None)
    flash.get("3") should equal(None)
  }

  test("values set with now are visible immediately") {
    flash.now("foo") = "baz"
    flash.get("foo") should equal(Some("baz"))
  }

  test("ungotten values set with now are removed on sweep") {
    flash.now("foo") = "baz"
    flash.sweep()
    flash.get("foo") should equal(None)
  }

  test("supports symbols as keys") {
    flash("foo") = "bar"
    flash.sweep()
    flash('foo) should equal("bar")
  }

  test("is serializable") {
    flash("foo") = "bar"
    val out = new ObjectOutputStream(new ByteArrayOutputStream)
    out.writeObject(flash)
  }

  test("flag marks all ungotten entries for sweeping") {
    flash("one") = 1
    flash.flag()
    flash.sweep()
    flash.get("one") should equal(None)
  }

  test("flag does not apply to entries added after flagging") {
    flash.flag()
    flash("one") = 1
    flash.sweep()
    flash.get("one") should equal(Some(1))
  }
}
