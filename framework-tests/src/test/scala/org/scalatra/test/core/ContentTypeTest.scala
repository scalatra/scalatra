package org.scalatra

import scala.actors.{Actor, TIMEOUT}
import scala.xml.Text
import java.net.URLEncoder
import test.{NettyBackend, AhcClientResponse}
import test.scalatest.ScalatraFunSuite
import com.ning.http.client.AsyncHttpClient

class ContentTypeTestServlet extends ScalatraApp {
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
    response.characterEncoding = "iso-8859-1"
    "test"
  }

  get("/implicit/byte-array") {
    "test".getBytes
  }

  get("/implicit/text-element") {
    Text("test")
  }

  import Actor._
  val conductor = actor {
    loop {
      reactWithin(10000) {
        case 1 =>
          val firstSender = sender
          reactWithin(10000) {
            case 2 =>
              firstSender ! 1
            case 'exit =>
              exit()
            case TIMEOUT =>
              firstSender ! "timed out"
            }
        case 'exit =>
          exit()
        case TIMEOUT =>
          sender ! "timed out"
      }
    }
  }

  get("/concurrent/1") {
    contentType = "1"
    // Wait for second request to complete
    (conductor !! 1)()

    200
  }

  get("/concurrent/2") {
    contentType = "2"
    // Let first request complete
    conductor ! 2
  }

  get("/default-charset") {
    contentType = "text/xml"
  }

  post("/echo") {
    params("echo")
  }

  conductor.start()
  override def destroy() { conductor ! 'exit }
}

abstract class ContentTypeTest extends ScalatraFunSuite {
  mount(new ContentTypeTestServlet)

  test("content-type test") {
    get("/json") {
      response.mediaType should equal (Some("application/json"))
    }

    get("/html") {
      response.mediaType should equal (Some("text/html"))
    }
  }

  test("contentType of a string defaults to text/plain") {
    get("/implicit/string") {
      response.mediaType should equal (Some("text/plain"))
    }
  }

  test("contentType of a byte array defaults to application/octet-stream") {
    get("/implicit/byte-array") {
      response.mediaType should equal (Some("application/octet-stream"))
    }
  }

  test("contentType of a text element defaults to text/html") {
    get("/implicit/text-element") {
      response.mediaType should equal (Some("text/html"))
    }
  }

  test("implicit content type does not override charset") {
    get("/implicit/string/iso-8859-1") {
      response.charset should equal (Some("ISO-8859-1"))
    }
  }

  test("contentType is threadsafe") {
    import Actor._

    def doRequest = actor {
      loop {
        react {
          case i: Int =>
            val http = new AsyncHttpClient
            val res = new AhcClientResponse(http.prepareGet("http://localhost:%s/concurrent/".format(backend.port) + i).execute().get())

            sender ! (i, res.mediaType)
            http.close()
            exit()
        }
      }
    }

    val futures = for (i <- 1 to 2) yield { doRequest !! i }
    for (future <- futures) {
      val result = future() match {
        case (i, mediaType) => mediaType should be (Some(i.toString))
      }
    }
  }

  test("charset is set to default when only content type is explicitly set") {
    get("/default-charset") {
      response.charset should equal (Some("UTF-8"))
    }
  }

  test("does not override request character encoding when explicitly set") {
    val charset = "iso-8859-5"
    val message = "Здравствуйте!"

    post("/echo",
         headers = Map("Content-Type" -> ("application/x-www-form-urlencoded; charset="+charset)),
         body = "echo="+URLEncoder.encode(message, charset)) {
      response.charset must be (Some(charset.toUpperCase))
      response.body must equal (message)
    }
  }
}

class NettyContentTypeTest extends ContentTypeTest with NettyBackend