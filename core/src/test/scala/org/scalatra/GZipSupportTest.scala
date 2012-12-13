package org.scalatra

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.matchers._

/**
 * Test servlet using GZipSupport.
 */
class GZipSupportTestServlet extends ScalatraServlet with GZipSupport {
  
  get("/") {
  	Helper.body
  }

  post("/") {
    Helper.body
  }
}

/**
 * Test suite.
 */
class GZipSupportTest extends ScalatraFunSuite with ShouldMatchers {
  addServlet(classOf[GZipSupportTestServlet], "/*")

  test("should return response gzipped") {
    session {
      get("/", Seq.empty, Map("Accept-Encoding" -> "gzip")) {
        header("Content-Encoding") should include("gzip")
        val uncompressed = Helper.uncompress(response.bodyBytes)
        uncompressed should equal(Helper.body);
      }
      
      post("/", Seq.empty, Map("Accept-Encoding" -> "gzip")) {
        header("Content-Encoding") should include("gzip")
        val uncompressed = Helper.uncompress(response.bodyBytes)
        uncompressed should equal(Helper.body);
      }
    }
  }
  
  test("should not return response gzipped") {
    session {
      get("/") {
        val contentEncoding = response.getHeader("Content-Encoding")
        assert(contentEncoding == null || !contentEncoding.contains("gzip"))
        body should equal(Helper.body);
      }
      
      post("/") {
        val contentEncoding = response.getHeader("Content-Encoding")
        assert(contentEncoding == null || !contentEncoding.contains("gzip"))
        body should equal(Helper.body);
      }
    }
  }
}

/**
 * Helper object for usage by tests.
 */
private object Helper {
  val body = "this is the body"
    
  /**
   * Uncompresses an array to a string with gzip.
   */
  def uncompress(bytes: Array[Byte]): String = {
    return convertStreamToString(new GZIPInputStream(new ByteArrayInputStream(bytes)))
  }
  
  /**
   * Returns a string with the content from the input stream.
   */
  private def convertStreamToString(is: InputStream): String = {
    val scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A")
    if (scanner.hasNext()) {
      scanner.next()
    } else {
      ""
    }
  }
}
