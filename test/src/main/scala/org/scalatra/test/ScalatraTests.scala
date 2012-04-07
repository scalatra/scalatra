package org.scalatra.test

import scala.collection.JavaConversions._
import scala.util.DynamicVariable
import org.eclipse.jetty.testing.HttpTester
import org.eclipse.jetty.testing.ServletTester
import java.net.HttpCookie
import java.util.Enumeration
import grizzled.slf4j.Logger
import org.eclipse.jetty.http.HttpHeaders
import org.eclipse.jetty.io.ByteArrayBuffer
import java.io._
import annotation.tailrec

/**
 * Provides a framework-agnostic way to test your Scalatra app.  You probably want to extend this with
 * either <code>org.scalatra.test.scalatest.ScalatraSuite</code> or
 * <code>org.scalatra.test.specs.ScalatraSpecification</code>.
 *
 * Cookies are crudely supported within session blocks.  No attempt is made
 * to match domains, paths, or max-ages; the request sends a Cookie header
 * to match whatever Set-Cookie call it received on the previous response.
 */
trait ScalatraTests extends JettyContainer with Client {
  private lazy val log = Logger(getClass)

  implicit def httpTesterToScalatraHttpTester(t: HttpTester) = new ScalatraHttpTester(t)

  def tester: ServletTester
  def servletContextHandler = tester.getContext

  private val _cookies = new DynamicVariable[Seq[HttpCookie]](Nil)
  private val _useSession = new DynamicVariable(false)

  protected def start() = tester.start()
  protected def stop() = tester.stop()

  type Response = HttpTester

  def submit[A](req: HttpTester)(f: => A): A = {
    val res = new HttpTester("iso-8859-1")

    req match {
      case multiPartReq: MultipartHttpTester => {
        // To preserve binary encoding of the request, we need to pass
        // the raw HTTP request as ByteArrayBuffer to ServletTester
        val reqBytes = multiPartReq.generateBytes()
        val resBytes = tester.getResponses(reqBytes)

        res.parse(resBytes.array(), req.getMethod == "HEAD")
      }

      case _ => {
        val reqString = req.generate
        log.debug("request\n"+reqString)
        val resString = tester.getResponses(req.generate)
        log.debug("response\n"+resString)
        res.parse(resString, req.getMethod == "HEAD")
      }
    }

    res.setContent(res.getContent match {
      case null => ""
      case content => content
    })
    if (_useSession.value && res.getHeader("Set-Cookie") != null) {
      val setCookies = res.getHeaderValues("Set-Cookie").asInstanceOf[Enumeration[String]]
      _cookies.value = setCookies flatMap { setCookie =>
        HttpCookie.parse(setCookie).iterator
      } toSeq
    }
    withResponse(res) { f }
  }

  def submit[A](method: String, uri: String, queryParams: Iterable[(String, String)] = Map.empty,
                headers: Map[String, String] = Map.empty, body: String = null)(f: => A): A = {
    val req = new HttpTester("iso-8859-1")
    req.setVersion("HTTP/1.0")
    req.setMethod(method)
    val queryString = toQueryString(queryParams)
    req.setURI(uri + (if (queryString == "") "" else "?") + queryString)
    req.setContent(body)
    headers.foreach(t => req.setHeader(t._1, t._2))
    _cookies.value foreach(c => req.addHeader("Cookie", c.toString))
    submit(req) { f }
  }


  protected def submitMultipart[A](method: String, uri: String, params: Iterable[(String, String)], headers: Map[String, String], files: Iterable[(String, File)])(f: => A) = {
    val boundary = "2ChY5dI4PKmv51s7Hs2n"
    val headersMulti = headers ++ Map(
      "Content-Type" -> ("multipart/form-data; boundary=" + boundary)
    )

    val req = new MultipartHttpTester()
    req.setVersion("HTTP/1.0")
    req.setMethod(method)
    req.setURI(uri)
    headersMulti.foreach(t => req.setHeader(t._1, t._2))
    _cookies.value foreach(c => req.addHeader("Cookie", c.toString))

    def genContent = {
      val out    = new ByteArrayOutputStream()
      val buffer = new Array[Byte](2048)

      @tailrec
      def copyStream(in: InputStream) {
        val bytesRead = in.read(buffer)

        if (bytesRead > 0) {
          out.write(buffer, 0, bytesRead)
          copyStream(in)
        }
      }

      def writeParamPart(param: (String, String)) {
        out.write(
          ("--" + boundary + "\r\n" +
           "Content-Disposition: form-data; name=\"" + param._1 + "\"\r\n\r\n" +
           param._2 + "\r\n"
          ).getBytes
        )
      }

      def writeFilePart(file: (String, File)) {
        out.write(
          ("--" + boundary + "\r\n" +
           "Content-Disposition: form-data; name=\"" + file._1 + "\"; filename=\"" + file._2.getName + "\"\r\n" +
           "Content-Type: application/octet-stream\r\n\r\n"
          ).getBytes
        )

        copyStream(new FileInputStream(file._2))
        out.write("\r\n".getBytes)
      }

      params.foreach(writeParamPart)
      files.foreach(writeFilePart)

      out.write(("--" + boundary + "--").getBytes)

      out.toByteArray
    }

    req.setContent(genContent)

    submit(req) { f }
  }

  def session[A](f: => A): A = {
    _cookies.withValue(Nil) {
      _useSession.withValue(true)(f)
    }
  }

  // shorthand for response.body
  def body = response.body
  // shorthand for response.header
  def header = response.header
  // shorthand for response.status
  def status = response.status

  // So servletContext.getRealPath doesn't crash.
  tester.setResourceBase("./src/main/webapp")
}

class MultipartHttpTester extends HttpTester {
  def setContent(content: Array[Byte]) {
    _genContent = content
    setLongHeader(HttpHeaders.CONTENT_LENGTH,_genContent.length);
  }

  def generateBytes(): ByteArrayBuffer = {
    val tempContent = _genContent
    _genContent = "".getBytes

    val rawHttp = generate()
    _genContent = tempContent

    val bytes = rawHttp.getBytes.toList ::: _genContent.toList

    new ByteArrayBuffer(bytes.toArray)
  }
}
