package org.scalatra

import _root_.akka.actor.{ Actor, Props, ActorRef, ActorSystem }
import _root_.akka.pattern.ask
import _root_.akka.util.Timeout
import scala.xml.Text
import test.scalatest.ScalatraFunSuite
import org.scalatra.util.RicherString._
import java.nio.charset.Charset
import scala.concurrent.duration._
import concurrent.Await
import org.scalatest.BeforeAndAfterAll
import org.eclipse.jetty.servlet.ServletHolder

class ContentTypeTestServlet(system: ActorSystem) extends ScalatraServlet {
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

  implicit val timeout: Timeout = 5 seconds

  val conductor = system.actorOf(Props(new Actor {

    var firstSender: ActorRef = _

    def receive = {
      case 1 =>
        firstSender = sender
        context.become(secondReceive)
    }

    def secondReceive: Receive = {
      case 2 => firstSender ! 1
    }
  }))

  get("/concurrent/1") {
    contentType = "1"
    // Wait for second request to complete
    conductor ? 1

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
}

class ContentTypeTest extends ScalatraFunSuite with BeforeAndAfterAll {
  val system = ActorSystem()
  implicit val timeout: Timeout = 5 seconds

  override def afterAll = system.shutdown()

  val servletHolder = new ServletHolder(new ContentTypeTestServlet(system))
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
      response.charset should equal(Some("ISO-8859-1"))
    }
  }

  test("contentType is threadsafe") {
    class RequestActor extends Actor {
      def receive = {
        case i: Int =>
          val res = get("/concurrent/" + i) { response }
          sender ! (i, res.mediaType)
      }
    }

    val futures = for (i <- 1 to 2) yield { system.actorOf(Props(new RequestActor)) ? i }
    for (future <- futures) {
      val (i: Int, mediaType: Option[String]) = Await.result(future.mapTo[(Int, Option[String])], 5 seconds)
      mediaType should be(Some(i.toString))
    }
  }

  test("charset is set to default when only content type is explicitly set") {
    get("/default-charset") {
      response.charset should equal(Some("UTF-8"))
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

