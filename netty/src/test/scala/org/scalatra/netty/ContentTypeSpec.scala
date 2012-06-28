package org.scalatra
package netty

import java.nio.charset.Charset
import actors.{TIMEOUT, Actor}
import xml.Text
import com.ning.http.client.AsyncHttpClient
import test.specs2.ScalatraSpec
import test.AhcClientResponse

class ContentTypeTestApp extends ScalatraApp {
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

  override def initialize(config: AppContext) = {
    super.initialize(config)
    conductor.start()
  }

  override def destroy() {
    conductor ! 'exit
  }
}

class ContentTypeSpec extends ScalatraSpec with NettyBackend {
  mount(new ContentTypeTestApp)

  def is = //sequential ^
    "To support content types the app should" ^
      "correctly tag a json response" ! jsonContentType ^
      "correctly tag a html response" ! htmlContentType ^
      "contentType of a string defaults to text/plain" ! stringDefaultsToPlain ^
      "contentType of a byte array defaults to application/octet-stream" ! bytesDefault ^
      "contentType of a text element defaults to text/html" ! textElementDefaultsHtml ^
      "implicit content type does not override charset" ! noImplicitCharsetOverride ^
      "charset is set to default when only content type is explicitly set" ! fallsbackDefaultCharset ^
      "contentType is threadsafe" ! contentTypeThreadSafety ^
    end

  def jsonContentType = {
    get("/json") {
      response.mediaType must beSome("application/json")
    }
  }

  def htmlContentType = {
    get("/html") {
      response.mediaType must beSome("text/html")
    }
  }

  def stringDefaultsToPlain = {
    get("/implicit/string") {
      response.mediaType must beSome("text/plain")
    }
  }

  def bytesDefault = {
    get("/implicit/byte-array") {
      response.mediaType must beSome("application/octet-stream")
    }
  }

  def textElementDefaultsHtml = {
    get("/implicit/text-element") {
      response.mediaType must beSome("text/html")
    }
  }

  def noImplicitCharsetOverride = {
    get("/implicit/string/iso-8859-1") {
      response.charset must beSome("ISO-8859-1")
    }
  }

  def fallsbackDefaultCharset = {
    get("/default-charset") {
      (response.charset must beSome("UTF-8"))
    }
  }

  def contentTypeThreadSafety = {
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
    val result = for (future <- futures) yield (future() match {
        case (i: Int, mediaType: Option[_]) => mediaType must beSome(i.toString)
      })
    result reduce (_ and _)
  }


}
