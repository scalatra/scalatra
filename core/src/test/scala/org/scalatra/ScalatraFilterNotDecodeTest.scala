package org.scalatra

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatra.test.scalatest.ScalatraFunSuite

class FilterTestNotDecodePathFilter extends ScalatraFilter {
  decodePercentEncodedPath = false

  get("/encoded-uri/:name") {
    println(requestPath)
    params("name")
  }

  get("/encoded-uri/:name1/:name2") {
    println(requestPath)
    s"'${params("name1")}' & '${params("name2")}'"
  }

}

@RunWith(classOf[JUnitRunner])
class FilterTestNotDecodePath extends ScalatraFunSuite {
  addFilter(classOf[FilterTestNotDecodePathFilter], "/*")

  test("handles encoded characters in uri but don't decode") {

    get("/encoded-uri/ac/dc") {
      status should equal(200)
      body should equal("'ac' & 'dc'")
    }

    // '%2F' is distinguished from '/'
    get("/encoded-uri/ac%2Fdc") {
      status should equal(200)
      body should equal("ac%2Fdc")
    }

    get("/encoded-uri/ö%C3%B6%25C3%25B6") {
      status should equal(200)
      body should equal("%C3%B6%C3%B6%25C3%25B6")
    }

    get("/encoded-uri/Fu%C3%9Fg%C3%A4nger%C3%BCberg%C3%A4nge%3F%23") {
      status should equal(200)
      body should equal("Fu%C3%9Fg%C3%A4nger%C3%BCberg%C3%A4nge%3F%23")
    }
  }
}

