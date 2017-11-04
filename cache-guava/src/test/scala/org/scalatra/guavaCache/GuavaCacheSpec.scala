package org.scalatra.guavaCache

import org.scalatra.test.scalatest.ScalatraFlatSpec

import java.time._

class GuavaCacheSpec extends ScalatraFlatSpec {
  "The GuavaCache" should "put and get a value" in {
    val cache = GuavaCache
    cache.put("key", "value", None)
    val get = cache.get("key")
    get should equal(Some("value"))
  }

  "The GuavaCache" should "not acquire discarded data" in {
    val cache = GuavaCache
    cache.put("key", "value", Some(Duration.ofMillis(1)))
    Thread.sleep(2)
    val get = cache.get("key")
    get shouldBe empty
  }
}
