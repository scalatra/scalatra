package org.scalatra

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest.matchers._
import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

/**
 * Test servlet using GZipSupport.
 */
class GZipSupportTestServlet extends ScalatraServlet with GZipSupportAppBase
trait GZipSupportAppBase extends ScalatraBase with GZipSupport {

  get("/") {
    Helper.body
  }

  post("/") {
    Helper.body
  }

  error {
    case t =>
      t.printStackTrace()
  }
}

/**
 * Test suite.
 */
class GZipSupportServletTest extends GZipSupportTest {
  mount(classOf[GZipSupportTestServlet], "/*")
}
abstract class GZipSupportTest extends ScalatraFunSuite with ShouldMatchers {


  test("should return response gzipped") {
    session {
      get("/", Seq.empty, Map("Accept-Encoding" -> "gzip")) {
        header("Content-Encoding") should include("gzip")
        val uncompressed = Helper.uncompress(response.bodyBytes)
        uncompressed should equal(Helper.body)
      }
      
      post("/", Seq.empty, Map("Accept-Encoding" -> "gzip")) {
        header("Content-Encoding") should include("gzip")
        val uncompressed = Helper.uncompress(response.bodyBytes)
        uncompressed should equal(Helper.body)
      }
    }
  }
  
  test("should not return response gzipped") {
    session {
      get("/") {
        val contentEncoding = response.getHeader("Content-Encoding")
        assert(contentEncoding == null || !contentEncoding.contains("gzip"))
        body should equal(Helper.body)
      }
      
      post("/") {
        val contentEncoding = response.getHeader("Content-Encoding")
        assert(contentEncoding == null || !contentEncoding.contains("gzip"))
        body should equal(Helper.body)
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
    convertStreamToString(new GZIPInputStream(new ByteArrayInputStream(bytes)))
  }
  
  /**
   * Returns a string with the content from the input stream.
   */
  private def convertStreamToString(is: InputStream): String = {
    val scanner = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A")
    if (scanner.hasNext) {
      scanner.next()
    } else {
      ""
    }
  }
}
