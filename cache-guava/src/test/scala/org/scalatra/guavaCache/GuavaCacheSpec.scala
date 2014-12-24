package org.scalatra.guavaCache

import org.scalatra.test.scalatest.ScalatraFlatSpec

class GuavaCacheSpec extends ScalatraFlatSpec {
  "The GuavaCache" should "put and get a value" in {
    val cache = new GuavaCache(5000)
    cache.put("key", "value")
    val get = cache.get("key")
    get should equal(Some("value"))
  }
}