package org.scalatra.cache

import org.scalatra.ScalatraServlet
import org.scalatra.test.scalatest.ScalatraFlatSpec

class CacheSupportSpec extends ScalatraFlatSpec {
  class TestServlet extends ScalatraServlet with CacheSupport {
    implicit val cacheBackend: Cache = new MapCache

    get("/") {
      cached(None) {
        <html><body>test</body></html>
      }
    }
  }

  addServlet(new TestServlet, "/")

  "The cache support" should "Return an ETag on first hit" in {
    get("/") {
      status should equal(200)
      response.getHeader("ETag") should not be (null)
    }
  }

  it should "Return a 304 on matching ETag" in {
    var etag: String = ""

    get("/") {
      status should equal(200)
      response.getHeader("ETag") should not be (null)
      etag = response.getHeader("ETag")
    }

    get("/", Map(), Map("ETag" -> etag)) {
      status should equal(304)
    }
  }
}
