package org.scalatra

import scala.actors.{Actor, TIMEOUT}
import scala.xml.Text
import org.scalatest.matchers.ShouldMatchers
import org.mortbay.jetty.testing.HttpTester
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

class ContentTypeTest extends ScalatraFunSuite with ShouldMatchers {
  val servletHolder = addServlet(classOf[ContentTypeTestServlet], "/*")
  servletHolder.setInitOrder(1) // force load on startup

  test("content-type test") {
    get("/json") {
      header("Content-Type") should equal ("application/json; charset=utf-8")
    }

    get("/html") {
      header("Content-Type") should equal ("text/html; charset=utf-8")
    }
  }

  test("contentType of a string defaults to text/plain") {
    get("/implicit/string") {
      header("Content-Type") should equal ("text/plain; charset=utf-8")
    }
  }

  test("contentType of a byte array defaults to application/octet-stream") {
    get("/implicit/byte-array") {
      header("Content-Type") should startWith ("application/octet-stream")
    }
  }

  test("contentType of a text element defaults to text/html") {
    get("/implicit/text-element") {
      header("Content-Type") should equal ("text/html; charset=utf-8")
    }
  }

  test("implicit content type does not override charset") {
    get("/implicit/string/iso-8859-1") {
      header("Content-Type") should equal ("text/plain; charset=iso-8859-1")
    }
  }

  test("contentType is threadsafe") {
    import Actor._
    import concurrent.MailBox
  
    val mailbox = new MailBox()
  
    def makeRequest(i: Int) = actor {
      val req = new HttpTester
      req.setVersion("HTTP/1.0")
      req.setMethod("GET")
      req.setURI("/concurrent/"+i)
      
      // Execute in own thread in servlet with LocalConnector
      val conn = tester.createLocalConnector()
      val res = new HttpTester
      res.parse(tester.getResponses(req.generate(), conn))
      mailbox.send((i, res.getHeader("Content-Type")))
    }

    makeRequest(1)
    makeRequest(2)
    var numReceived = 0
    while (numReceived < 2) {
      mailbox.receiveWithin(10000) {
        case (i, contentType: String) =>
          contentType.split(";")(0) should be (i.toString)
          numReceived += 1

        case TIMEOUT =>
          fail("Timed out")
      }
    }
  }

  test("charset is set to default when only content type is explicitly set") {
    get("/default-charset") {
      header("Content-Type") should equal ("text/xml; charset=utf-8")
    }
  }

  test("does not override request character encoding when explicitly set") {
    val charset = "iso-8859-5"
    val message = "Здравствуйте!"

    val req = new HttpTester(Charset.defaultCharset.name)
    req.setVersion("HTTP/1.0")
    req.setMethod("POST")
    req.setURI("/echo")
    req.setHeader("Content-Type", "application/x-www-form-urlencoded; charset="+charset)
    req.setContent("echo="+URLEncoder.encode(message, charset))

    val res = new HttpTester(Charset.defaultCharset.name)
    res.parse(tester.getResponses(req.generate()))
    res.getContent should equal(message)
  }
}

