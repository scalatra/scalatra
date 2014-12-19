package org.scalatra

import java.io._

import org.apache.http.impl.client.{ CloseableHttpClient, HttpClientBuilder }
import org.scalatest._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Test servlet using ContentEncodingSupport.
 */
class ContentEncodingSupportTestServlet extends ScalatraServlet with ContentEncodingSupportAppBase {
  implicit protected def executor: ExecutionContext = ExecutionContext.global
}

trait ContentEncodingSupportAppBase extends ScalatraBase with FutureSupport with ContentEncodingSupport {
  get("/") {
    Helper.body
  }

  post("/") {
    request.body
  }

  get("/async") {
    Future(Helper.body)
  }
}

/** Test suite for `deflate`. */
class DeflateSupportServletTest extends ContentEncodingSupportTest(ContentEncoding.Deflate) {
  mount(classOf[ContentEncodingSupportTestServlet], "/*")
}

/** Test suite for `gzip`. */
class GZipSupportServletTest extends ContentEncodingSupportTest(ContentEncoding.GZip) {
  mount(classOf[ContentEncodingSupportTestServlet], "/*")
}

/** Abstract test suite, for any encoding. */
abstract class ContentEncodingSupportTest(e: ContentEncoding) extends ScalatraFunSuite with Matchers {
  implicit val encoding = e

  test("should decode request if Content-Encoding is supported") {
    post("/", Helper.compress(Helper.body), Map("Content-Encoding" -> encoding.name)) {
      response.body should equal(Helper.body)
    }
  }

  test("should encode response if Accept-Encoding is supported") {
    session {
      get("/", Seq.empty, Map("Accept-Encoding" -> encoding.name)) {
        Helper.uncompress(response.bodyBytes) should equal(Helper.body)
        response.getHeader("Content-Encoding") should equal(encoding.name)
      }

      post("/", Helper.body, Map("Accept-Encoding" -> encoding.name)) {
        response.headers("Content-Encoding") should equal(List(encoding.name))
        Helper.uncompress(response.bodyBytes) should equal(Helper.body)
      }
    }
  }

  test("should not encode response if accept-encoding is not supported") {
    session {
      get("/", Seq.empty, Map("Accept-Encoding" -> "foobar")) {
        response.getHeader("Content-Encoding") should be(null)
        body should equal(Helper.body)
      }

      post("/", Helper.body, Map("Accept-Encoding" -> "foobar")) {
        response.getHeader("Content-Encoding") should be(null)
        body should equal(Helper.body)
      }
    }
  }

  test("should encode async response") {
    get("/async", Seq.empty, Map("Accept-Encoding" -> encoding.name)) {
      response.getHeader("Content-Encoding") should equal(encoding.name)
      Helper.uncompress(response.bodyBytes) should equal(Helper.body)
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

  def compress(str: String)(implicit encoding: ContentEncoding): Array[Byte] = {
    val out = new ByteArrayOutputStream()

    val zout = new BufferedWriter(new OutputStreamWriter(encoding.encode(out), "UTF-8"))
    zout.write(str)
    zout.close()

    out.toByteArray
  }

  /**
   * Uncompresses an array to a string with gzip.
   */
  def uncompress(bytes: Array[Byte])(implicit encoding: ContentEncoding): String = {
    convertStreamToString(encoding.decode(new ByteArrayInputStream(bytes)))
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
