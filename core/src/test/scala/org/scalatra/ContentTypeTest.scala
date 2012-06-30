package org.scalatra

import scala.actors.{Actor, TIMEOUT}
import scala.xml.Text
import org.eclipse.jetty.testing.HttpTester
import java.net.URLEncoder
import java.nio.charset.Charset
import test.scalatest.ScalatraFunSuite

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

  override def init () { conductor.start() }
  override def destroy() { conductor ! 'exit }
}

class ContentTypeTest extends ScalatraFunSuite {
  val servletHolder = addServlet(classOf[ContentTypeTestServlet], "/*")
  servletHolder.setInitOrder(1) // force load on startup

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

  /*
  test("contentType is threadsafe") {
    import Actor._

    def doRequest = actor {
      loop {
        react {
          case i: Int =>
            val req = new HttpTester
            req.setVersion("HTTP/1.0")
            req.setMethod("GET")
            req.setURI("/concurrent/"+i)
            // Execute in own thread in servlet with LocalConnector
            val conn = tester.createLocalConnector()
            val res = new HttpTester
            val resString = tester.getResponses(req.generate(), conn)
            res.parse(resString)
            sender ! (i, res.mediaType)
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
  }*/

  test("charset is set to default when only content type is explicitly set") {
    get("/default-charset") {
      response.charset should equal (Some("UTF-8"))
    }
  }

  test("does not override request character encoding when explicitly set") {
    val charset = "iso-8859-5"
    val message = "Здравствуйте!"

    post(
      "/echo",
      headers = Map("Content-Type" -> ("application/x-www-form-urlencoded; charset=" + charset)),
      body = "echo="+URLEncoder.encode(message, charset))
    {
      body should equal(message)
    }
/*
    val req = new HttpTester("iso-8859-1")
    req.setVersion("HTTP/1.0")
    req.setMethod("POST")
    req.setURI("/echo")
    req.setHeader("Content-Type", "application/x-www-form-urlencoded; charset="+charset)
    req.setContent("echo="+URLEncoder.encode(message, charset))
    println(req.generate())

    val res = new HttpTester("iso-8859-1")
    res.parse(tester.getResponses(req.generate()))
    println(res.getCharacterEncoding)
    res.getContent should equal(message)*/
  }
}

