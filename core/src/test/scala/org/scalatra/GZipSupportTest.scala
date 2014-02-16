package org.scalatra

import scala.concurrent.{ExecutionContext, Future}
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatest._
import org.apache.http.impl.client.{HttpClientBuilder, CloseableHttpClient}

/**
 * Test servlet using GZipSupport.
 */
class GZipSupportTestServlet extends ScalatraServlet with GZipSupportAppBase {
  implicit protected def executor: ExecutionContext = ExecutionContext.global
}
trait GZipSupportAppBase extends ScalatraBase with FutureSupport with GZipSupport {

  get("/") {
    Helper.body
  }

  post("/") {
    Helper.body
  }

  get("/async") {
    Future(Helper.body)
  }
}

/**
 * Test suite.
 */
class GZipSupportServletTest extends GZipSupportTest {
  mount(classOf[GZipSupportTestServlet], "/*")
}
abstract class GZipSupportTest extends ScalatraFunSuite with Matchers {



  test("should return response gzipped if accept-encoding includes gzip") {
    session {
      get("/", Seq.empty, Map("Accept-Encoding" -> "gzip")) {
        val uncompressed = Helper.uncompress(response.bodyBytes)
        uncompressed should equal(Helper.body)
        response.getHeader("Content-Encoding") should equal("gzip")
      }
      
      post("/", Seq.empty, Map("Accept-Encoding" -> "gzip, deflate, sdch")) {
        response.headers("Content-Encoding") should equal(List("gzip"))
        val uncompressed = Helper.uncompress(response.bodyBytes)
        uncompressed should equal(Helper.body)
      }
    }
  }
  
  test("should not return response gzipped if accept-encoding does not include gzip") {
    session {
      get("/") {
        val contentEncoding = response.getHeader("Content-Encoding")
        contentEncoding should be (null)
        body should equal(Helper.body)
      }

      post("/") {
        val contentEncoding = response.getHeader("Content-Encoding")
        contentEncoding should be (null)
        body should equal(Helper.body)
      }
    }
  }

  test("should return async response gzipped") {
    get("/async", Seq.empty, Map("Accept-Encoding" -> "gzip")) {
      response.getHeader("Content-Encoding") should equal("gzip")
      val uncompressed = Helper.uncompress(response.bodyBytes)
      uncompressed should equal(Helper.body)
    }
  }

  override protected def createClient: CloseableHttpClient = {
    val builder = HttpClientBuilder.create()
    builder.disableRedirectHandling()
    builder.disableContentCompression()
    builder.build()
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
