package org.scalatra

import java.nio.charset.Charset

import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.scalatest.BeforeAndAfterAll
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.scalatra.util.RicherString._

import scala.xml.Text

class ContentTypeTestServlet extends ScalatraServlet {
  get("/json") {
    contentType = "application/json; charset=utf-8"
    """{msg: "test"}"""
  }

  get("/html") {
    contentType = "text/html; charset=utf-8"
    "test"
  }

  get("/implicit/string") {
    "test"
  }

  get("/implicit/string/iso-8859-1") {
    response.setCharacterEncoding("iso-8859-1")
    "test"
  }

  get("/implicit/byte-array") {
    Array[Byte]()
  }

  get("/implicit/byte-array-text") {
    "Здравствуйте!".getBytes("iso-8859-5")
  }

  get("/implicit/text-element") {
    Text("test")
  }

  get("/default-charset") {
    contentType = "text/xml"
  }

  post("/echo") {
    params("echo")
  }
}

class ContentTypeTest extends ScalatraFunSuite with BeforeAndAfterAll {

  val servletHolder = new ServletHolder(new ContentTypeTestServlet)
  servletHolder.setInitOrder(1) // force load on startup
  servletContextHandler.addServlet(servletHolder, "/*")

  test("content-type test") {
    get("/json") {
      response.mediaType should equal(Some("application/json"))
    }

    get("/html") {
      response.mediaType should equal(Some("text/html"))
    }
  }

  test("contentType of a string defaults to text/plain") {
    get("/implicit/string") {
      response.mediaType should equal(Some("text/plain"))
    }
  }

  test("contentType of a byte array defaults to application/octet-stream") {
    get("/implicit/byte-array") {
      response.mediaType should equal(Some("application/octet-stream"))
    }
  }

  test("contentType of a byte array with text content detects text/plain; charset=iso-8859-5") {
    get("/implicit/byte-array-text") {
      response.charset should equal(Some("ISO-8859-5"))
      response.mediaType should equal(Some("text/plain"))
    }
  }

  test("contentType of a text element defaults to text/html") {
    get("/implicit/text-element") {
      response.mediaType should equal(Some("text/html"))
    }
  }

  test("implicit content type does not override charset") {
    get("/implicit/string/iso-8859-1") {
      response.charset should equal(Some("iso-8859-1"))
    }
  }

  test("charset is set to default when only content type is explicitly set") {
    get("/default-charset") {
      response.charset should equal(Some("utf-8"))
    }
  }

  test("does not override request character encoding when explicitly set") {
    val charset = "iso-8859-5"
    val message = "Здравствуйте!"

    post(
      "/echo",
      headers = Map("Content-Type" -> ("application/x-www-form-urlencoded; charset=" + charset)),
      body = ("echo=" + message.urlEncode(Charset.forName(charset)))) {
        body should equal(message)
      }
  }
}
